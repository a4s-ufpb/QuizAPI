package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Tag;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.TagRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagServiceTest {
    @Mock TagRepository repository;
    @Mock UserService userService;
    @InjectMocks TagService tagService;

    User owner;
    User other;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        owner = new User(UUID.randomUUID(), "Owner", "o@o.com", "12345678", Role.USER);
        other = new User(UUID.randomUUID(), "Other", "x@x.com", "12345678", Role.USER);
    }

    @Test
    void create_newName_savesAndReturnsResponse() {
        when(userService.findUserByToken("t")).thenReturn(owner);
        when(repository.findByCreatorAndNameIgnoreCase(owner, "javascript")).thenReturn(null);
        when(repository.save(any(Tag.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TagResponse res = tagService.create(new TagRequest("javascript"), "t");

        assertEquals("javascript", res.name());
        assertEquals(owner.getUuid(), res.creatorId());
        verify(repository).save(any(Tag.class));
    }

    @Test
    void create_alreadyExists_returnsExistingWithoutSaving() {
        when(userService.findUserByToken("t")).thenReturn(owner);
        Tag existing = new Tag("java", owner);
        when(repository.findByCreatorAndNameIgnoreCase(owner, "java")).thenReturn(existing);

        TagResponse res = tagService.create(new TagRequest("java"), "t");

        assertEquals("java", res.name());
        verify(repository, never()).save(any());
    }

    @Test
    void update_notOwner_throwsPermission() {
        when(userService.findUserByToken("t")).thenReturn(other);
        Tag t = new Tag("foo", owner);
        when(repository.findById(1L)).thenReturn(Optional.of(t));

        assertThrows(UserNotHavePermissionException.class,
                () -> tagService.update(1L, new TagRequest("bar"), "t"));
    }

    @Test
    void delete_owner_deletes() throws UserNotHavePermissionException {
        when(userService.findUserByToken("t")).thenReturn(owner);
        Tag t = new Tag("foo", owner);
        when(repository.findById(1L)).thenReturn(Optional.of(t));

        tagService.delete(1L, "t");

        verify(repository).delete(t);
    }

    @Test
    void listAll_returnsSortedByName() {
        Tag zoo = new Tag("Zoo", owner);
        Tag arte = new Tag("arte", other);
        when(repository.findAll()).thenReturn(List.of(zoo, arte));

        List<TagResponse> result = tagService.listAll();

        assertEquals(2, result.size());
        assertEquals("arte", result.get(0).name());
        assertEquals("Zoo", result.get(1).name());
    }
}
