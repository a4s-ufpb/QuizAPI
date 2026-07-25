package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Tag;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.TagRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TagService {
    private final TagRepository repository;
    private final UserService userService;

    public TagService(TagRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public TagResponse create(TagRequest req, String token) {
        User user = userService.findUserByToken(token);
        Tag existing = repository.findByCreatorAndNameIgnoreCase(user, req.name());
        if (existing != null) {
            return existing.entityToResponse();
        }
        Tag tag = new Tag(req.name().trim(), user);
        return repository.save(tag).entityToResponse();
    }

    public List<TagResponse> listMine(String token) {
        User user = userService.findUserByToken(token);
        return repository.findByCreatorOrderByNameAsc(user).stream().map(Tag::entityToResponse).toList();
    }

    public List<TagResponse> listByCreator(UUID creatorUuid) {
        return repository.findByCreator_UuidOrderByNameAsc(creatorUuid).stream().map(Tag::entityToResponse).toList();
    }

    public TagResponse update(Long id, TagRequest req, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        Tag tag = repository.findById(id).orElseThrow(() -> new RuntimeException("Tag não encontrada"));
        if (user.userNotHavePermission(tag.getCreator())) {
            throw new UserNotHavePermissionException("Sem permissão para atualizar essa tag");
        }
        tag.setName(req.name().trim());
        return repository.save(tag).entityToResponse();
    }

    public void delete(Long id, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        Tag tag = repository.findById(id).orElseThrow(() -> new RuntimeException("Tag não encontrada"));
        if (user.userNotHavePermission(tag.getCreator())) {
            throw new UserNotHavePermissionException("Sem permissão para remover essa tag");
        }
        repository.delete(tag);
    }
}
