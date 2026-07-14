package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.MaterialRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Material;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ImageSizeLimitExceededException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeAlreadyExistsException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import br.ufpb.dcx.apps4society.quizapi.util.ImageValidator;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ThemeService {
    private static final String THEME_IMAGE_PREFIX = "themes/";

    private ThemeRepository repository;
    private UserService userService;
    private ImageStorageService imageStorageService;

    @Autowired
    public ThemeService(ThemeRepository repository, UserService userService, ImageStorageService imageStorageService) {
        this.repository = repository;
        this.userService = userService;
        this.imageStorageService = imageStorageService;
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
        user.addTheme(saveTheme);

        repository.save(saveTheme);
        return saveTheme.entityToResponse();
    }

    /** Substitui os materiais do tema pelos informados na requisição. */
    private void applyMaterials(Theme theme, List<MaterialRequest> materials) {
        List<Material> mapped = materials.stream()
                .map(m -> new Material(m.name(), m.link(), m.type(), theme))
                .toList();
        theme.replaceMaterials(mapped);
    }

    // URL já armazenada (edição sem trocar a imagem) não conta pro limite —
    // só payload base64 novo passa por aqui de fato.
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
    }

    // Temas são lidos com muito mais frequência do que escritos; cacheado por
    // (página, tamanho, nome) com TTL curto (ver spring.cache.caffeine.spec).
    @Cacheable(value = "themes", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #name")
    public Page<ThemeResponse> findAllThemes(Pageable pageable, String name){
        Page<Theme> themes;

        if (name.isBlank()){
            themes = repository.findAll(pageable);
        } else {
            themes = repository.findByNameStartsWithIgnoreCase(name, pageable);
        }

        if (themes.isEmpty()){
            throw new ThemeNotFoundException("Nenhum tema foi cadastrado");
        }

        return themes.map(Theme::entityToResponse);
    }

    public ThemeResponse findThemeById(Long id){
        return repository.findById(id)
                .orElseThrow(() -> new ThemeNotFoundException(Messages.THEME_NOT_FOUND))
                .entityToResponse();
    }

    @Cacheable(value = "themes", key = "'creator:' + #token + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #name")
    public Page<ThemeResponse> findThemesByCreator(String token, String name, Pageable pageable){
        User creator = userService.findUserByToken(token);
        Page<Theme> themes = null;

        if (name.isBlank()){
            themes = repository.findByCreator(creator, pageable);
        } else {
            themes = repository.findByCreatorAndNameStartsWithIgnoreCase(creator, name, pageable);
        }

        if (themes.isEmpty()){
            throw new ThemeNotFoundException("Nenhum Tema encontrado");
        }

        return themes.map(Theme::entityToResponse);
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

        return theme.entityToResponse();
    }

    private void updateData(Theme theme, ThemeUpdate themeUpdate, String imageUrl){
        theme.setName(themeUpdate.name());
        theme.setImageUrl(imageUrl);
        theme.setDescription(themeUpdate.description());
        applyMaterials(theme, themeUpdate.materialsOrEmpty());
    }
}
