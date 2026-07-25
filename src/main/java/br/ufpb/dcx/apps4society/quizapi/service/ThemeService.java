package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.MaterialRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Material;
import br.ufpb.dcx.apps4society.quizapi.entity.Tag;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.ThemeLike;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.TagRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeLikeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ImageSizeLimitExceededException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeAlreadyExistsException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import br.ufpb.dcx.apps4society.quizapi.util.ImageValidator;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ThemeService {
    private static final Logger log = LoggerFactory.getLogger(ThemeService.class);
    private static final String THEME_IMAGE_PREFIX = "themes/";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ThemeRepository repository;
    private final UserService userService;
    private final ImageStorageService imageStorageService;
    private final TagRepository tagRepository;
    private final ThemeLikeRepository themeLikeRepository;

    @Autowired
    public ThemeService(ThemeRepository repository, UserService userService,
                        ImageStorageService imageStorageService,
                        TagRepository tagRepository,
                        ThemeLikeRepository themeLikeRepository) {
        this.repository = repository;
        this.userService = userService;
        this.imageStorageService = imageStorageService;
        this.tagRepository = tagRepository;
        this.themeLikeRepository = themeLikeRepository;
    }

    @CacheEvict(value = "themes", allEntries = true)
    public ThemeResponse insertTheme(ThemeRequest themeRequest, String token) throws ThemeAlreadyExistsException, ImageSizeLimitExceededException {
        Theme theme = repository.findByNameIgnoreCase(themeRequest.name());

        if (theme != null){
            throw new ThemeAlreadyExistsException("Esse tema já foi cadastrado, tente novamente com outro Nome");
        }

        User user = userService.findUserByToken(token);

        validateImage(themeRequest.imageUrl());
        String imageUrl = imageStorageService.upload(themeRequest.imageUrl(), THEME_IMAGE_PREFIX);

        Theme saveTheme = new Theme(themeRequest.name(), imageUrl, themeRequest.description(), user);
        applyMaterials(saveTheme, themeRequest.materialsOrEmpty());
        applyTags(saveTheme, themeRequest.tagIdsOrEmpty(), user);
        user.addTheme(saveTheme);

        repository.save(saveTheme);
        log.info("tema criado: '{}' por {}", saveTheme.getName(), user.getEmail());
        return toResponse(saveTheme, user.getUuid());
    }

    @CacheEvict(value = "themes", allEntries = true)
    public ThemeResponse insertThemeMultipart(String name, String description, String materialsJson, String tagIdsJson,
                                               MultipartFile imageFile, String imageUrl, String token)
            throws ThemeAlreadyExistsException {
        Theme existing = repository.findByNameIgnoreCase(name);
        if (existing != null) {
            throw new ThemeAlreadyExistsException("Esse tema já foi cadastrado, tente novamente com outro Nome");
        }

        User user = userService.findUserByToken(token);

        String finalImageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            finalImageUrl = imageStorageService.upload(imageFile, THEME_IMAGE_PREFIX);
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            finalImageUrl = imageStorageService.upload(imageUrl, THEME_IMAGE_PREFIX);
        } else {
            finalImageUrl = null;
        }

        List<MaterialRequest> materials = parseMaterials(materialsJson);
        List<Long> tagIds = parseTagIds(tagIdsJson);
        Theme saveTheme = new Theme(name, finalImageUrl, description, user);
        applyMaterials(saveTheme, materials);
        applyTags(saveTheme, tagIds, user);
        user.addTheme(saveTheme);
        repository.save(saveTheme);
        log.info("tema criado (multipart): '{}' por {}", name, user.getEmail());
        return toResponse(saveTheme, user.getUuid());
    }

    @CacheEvict(value = "themes", allEntries = true)
    public ThemeResponse updateThemeMultipart(Long id, String name, String description, String materialsJson, String tagIdsJson,
                                               MultipartFile imageFile, String imageUrl, String token)
            throws UserNotHavePermissionException, ThemeAlreadyExistsException {
        User user = userService.findUserByToken(token);

        Theme theme = repository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));

        if (user.userNotHavePermission(theme.getCreator())) {
            throw new UserNotHavePermissionException("Usuário não tem permissão para atualizar esse tema");
        }

        Theme themeTestName = repository.findByNameIgnoreCase(name);
        if (!theme.equals(themeTestName) && themeTestName != null) {
            throw new ThemeAlreadyExistsException("Esse tema já foi cadastrado, tente novamente com outro Nome");
        }

        String finalImageUrl;
        if (imageFile != null && !imageFile.isEmpty()) {
            finalImageUrl = imageStorageService.upload(imageFile, THEME_IMAGE_PREFIX);
        } else if (imageUrl != null && !imageUrl.isBlank()) {
            finalImageUrl = imageStorageService.upload(imageUrl, THEME_IMAGE_PREFIX);
        } else {
            finalImageUrl = theme.getImageUrl();
        }

        List<MaterialRequest> materials = parseMaterials(materialsJson);
        List<Long> tagIds = parseTagIds(tagIdsJson);
        theme.setName(name);
        theme.setImageUrl(finalImageUrl);
        theme.setDescription(description);
        applyMaterials(theme, materials);
        applyTags(theme, tagIds, theme.getCreator());
        repository.save(theme);
        log.info("tema atualizado (multipart): id={} '{}' por {}", id, name, user.getEmail());
        return toResponse(theme, user.getUuid());
    }

    private List<MaterialRequest> parseMaterials(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<MaterialRequest>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> parseTagIds(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void applyMaterials(Theme theme, List<MaterialRequest> materials) {
        List<Material> mapped = materials.stream()
                .map(m -> new Material(m.name(), m.link(), m.type(), theme))
                .toList();
        theme.replaceMaterials(mapped);
    }

    private void applyTags(Theme theme, List<Long> tagIds, User owner) {
        if (tagIds == null) tagIds = List.of();
        List<Tag> tags = tagIds.isEmpty() ? List.of() : tagRepository.findByIdIn(tagIds);
        Set<Tag> filtered = new HashSet<>();
        for (Tag t : tags) {
            if (t.getCreator().getUuid().equals(owner.getUuid())) {
                filtered.add(t);
            }
        }
        theme.setTags(filtered);
    }

    private void validateImage(String imageBase64OrUrl) throws ImageSizeLimitExceededException {
        boolean isStoredUrl = imageBase64OrUrl != null
                && (imageBase64OrUrl.startsWith("http://") || imageBase64OrUrl.startsWith("https://"));
        int size = isStoredUrl ? 0 : ImageValidator.decodedSizeInBytes(imageBase64OrUrl);

        if (size > ImageValidator.MAX_THEME_IMAGE_SIZE_BYTES) {
            throw new ImageSizeLimitExceededException("A imagem do tema deve ter no máximo 1MB");
        }
    }

    @CacheEvict(value = "themes", allEntries = true)
    public void removeTheme(Long id, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);

        Theme theme = repository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));

        if (user.userNotHavePermission(theme.getCreator())) {
            throw new UserNotHavePermissionException("Usuário não tem permissão para remover esse tema");
        }

        repository.delete(theme);
        log.info("tema removido: id={} '{}' por {}", id, theme.getName(), user.getEmail());
    }

    public Page<ThemeResponse> findAllThemes(Pageable pageable, String name, List<Long> tagIds, String token) {
        Page<Theme> themes;

        boolean filterTags = tagIds != null && !tagIds.isEmpty();
        if (filterTags) {
            themes = repository.findByTagIds(tagIds, name == null ? "" : name, pageable);
        } else if (name == null || name.isBlank()) {
            themes = repository.findAll(pageable);
        } else {
            themes = repository.findByNameStartsWithIgnoreCase(name, pageable);
        }

        if (themes.isEmpty()){
            throw new ThemeNotFoundException("Nenhum tema foi cadastrado");
        }

        UUID viewerUuid = safeViewer(token);
        return themes.map(t -> toResponse(t, viewerUuid));
    }

    private UUID safeViewer(String token) {
        if (token == null || token.isBlank()) return null;
        try { return userService.findUserByToken(token).getUuid(); }
        catch (Exception e) { return null; }
    }

    public ThemeResponse findThemeById(Long id){
        Theme t = repository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));
        return toResponse(t, null);
    }

    public ThemeResponse findThemeById(Long id, String token) {
        Theme t = repository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));
        return toResponse(t, safeViewer(token));
    }

    public Page<ThemeResponse> findThemesByCreator(String token, String name, Pageable pageable){
        User creator = userService.findUserByToken(token);
        Page<Theme> themes;

        if (name.isBlank()){
            themes = repository.findByCreator(creator, pageable);
        } else {
            themes = repository.findByCreatorAndNameStartsWithIgnoreCase(creator, name, pageable);
        }

        if (themes.isEmpty()){
            throw new ThemeNotFoundException("Nenhum Tema encontrado");
        }

        UUID viewerUuid = creator.getUuid();
        return themes.map(t -> toResponse(t, viewerUuid));
    }

    @CacheEvict(value = "themes", allEntries = true)
    public ThemeResponse updateTheme(Long id, ThemeUpdate themeUpdate, String token) throws UserNotHavePermissionException, ThemeAlreadyExistsException, ImageSizeLimitExceededException {
        User user = userService.findUserByToken(token);

        Theme theme = repository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));

        if (user.userNotHavePermission(theme.getCreator())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para atualizar esse tema");
        }

        Theme themeTestName = repository.findByNameIgnoreCase(themeUpdate.name());

        if (!theme.equals(themeTestName) && themeTestName != null){
            throw new ThemeAlreadyExistsException("Esse tema já foi cadastrado, tente novamente com outro Nome");
        }

        validateImage(themeUpdate.imageUrl());
        String imageUrl = imageStorageService.upload(themeUpdate.imageUrl(), THEME_IMAGE_PREFIX);

        updateData(theme, themeUpdate, imageUrl);
        repository.save(theme);
        log.info("tema atualizado: id={} '{}' por {}", id, themeUpdate.name(), user.getEmail());
        return toResponse(theme, user.getUuid());
    }

    private void updateData(Theme theme, ThemeUpdate themeUpdate, String imageUrl){
        theme.setName(themeUpdate.name());
        theme.setImageUrl(imageUrl);
        theme.setDescription(themeUpdate.description());
        applyMaterials(theme, themeUpdate.materialsOrEmpty());
        applyTags(theme, themeUpdate.tagIdsOrEmpty(), theme.getCreator());
    }

    // === Likes ===
    @CacheEvict(value = "themes", allEntries = true)
    public ThemeResponse likeTheme(Long themeId, String token) {
        User user = userService.findUserByToken(token);
        Theme theme = repository.findById(themeId)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));
        if (!themeLikeRepository.existsByUser_UuidAndTheme_Id(user.getUuid(), themeId)) {
            themeLikeRepository.save(new ThemeLike(user, theme));
        }
        return toResponse(theme, user.getUuid());
    }

    @CacheEvict(value = "themes", allEntries = true)
    public ThemeResponse unlikeTheme(Long themeId, String token) {
        User user = userService.findUserByToken(token);
        Theme theme = repository.findById(themeId)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND));
        themeLikeRepository.findByUser_UuidAndTheme_Id(user.getUuid(), themeId)
                .ifPresent(themeLikeRepository::delete);
        return toResponse(theme, user.getUuid());
    }

    private ThemeResponse toResponse(Theme theme, UUID viewerUuid) {
        long count = themeLikeRepository.countByTheme_Id(theme.getId());
        boolean liked = viewerUuid != null && themeLikeRepository.existsByUser_UuidAndTheme_Id(viewerUuid, theme.getId());
        return theme.entityToResponse(count, liked);
    }
}
