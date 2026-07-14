package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.friendship.FriendshipResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Friendship;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.FriendshipStatus;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.FriendshipRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.FriendshipNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendshipServiceTest {
    @Mock FriendshipRepository friendshipRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @InjectMocks FriendshipService friendshipService;

    User requester;
    User addressee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        requester = new User(UUID.randomUUID(), "Requester", "r@r.com", "12345678", Role.USER);
        addressee = new User(UUID.randomUUID(), "Addressee", "a@a.com", "12345678", Role.USER);
    }

    @Test
    void requestFriendship_new_savesPending() {
        when(userService.findUserByToken("token")).thenReturn(requester);
        when(userRepository.findById(addressee.getUuid())).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findBetween(requester, addressee)).thenReturn(Optional.empty());

        FriendshipResponse response = friendshipService.requestFriendship(addressee.getUuid(), "token");

        assertNotNull(response);
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    void requestFriendship_toSelf_throws() {
        when(userService.findUserByToken("token")).thenReturn(requester);
        when(userRepository.findById(requester.getUuid())).thenReturn(Optional.of(requester));

        assertThrows(IllegalArgumentException.class,
                () -> friendshipService.requestFriendship(requester.getUuid(), "token"));
    }

    @Test
    void requestFriendship_existing_returnsWithoutSaving() {
        Friendship existing = new Friendship(requester, addressee);
        when(userService.findUserByToken("token")).thenReturn(requester);
        when(userRepository.findById(addressee.getUuid())).thenReturn(Optional.of(addressee));
        when(friendshipRepository.findBetween(requester, addressee)).thenReturn(Optional.of(existing));

        FriendshipResponse response = friendshipService.requestFriendship(addressee.getUuid(), "token");

        assertNotNull(response);
        verify(friendshipRepository, never()).save(any());
    }

    @Test
    void acceptFriendship_byAddressee_setsAccepted() throws UserNotHavePermissionException {
        Friendship friendship = new Friendship(requester, addressee);
        when(userService.findUserByToken("token")).thenReturn(addressee);
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(friendship));

        friendshipService.acceptFriendship(1L, "token");

        assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
        verify(friendshipRepository).save(friendship);
    }

    @Test
    void acceptFriendship_notAddressee_throws() {
        Friendship friendship = new Friendship(requester, addressee);
        when(userService.findUserByToken("token")).thenReturn(requester); // não é o destinatário
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(friendship));

        assertThrows(UserNotHavePermissionException.class,
                () -> friendshipService.acceptFriendship(1L, "token"));
    }

    @Test
    void acceptFriendship_notFound_throws() {
        when(userService.findUserByToken("token")).thenReturn(addressee);
        when(friendshipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FriendshipNotFoundException.class,
                () -> friendshipService.acceptFriendship(99L, "token"));
    }

    @Test
    void removeFriendship_notInvolved_throws() {
        User stranger = new User(UUID.randomUUID(), "Stranger", "s@s.com", "12345678", Role.USER);
        Friendship friendship = new Friendship(requester, addressee);
        when(userService.findUserByToken("token")).thenReturn(stranger);
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(friendship));

        assertThrows(UserNotHavePermissionException.class,
                () -> friendshipService.removeFriendship(1L, "token"));
        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    void removeFriendship_involved_deletes() throws UserNotHavePermissionException {
        Friendship friendship = new Friendship(requester, addressee);
        when(userService.findUserByToken("token")).thenReturn(requester);
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(friendship));

        friendshipService.removeFriendship(1L, "token");

        verify(friendshipRepository).delete(friendship);
    }
}
