package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.friendship.FriendshipResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Friendship;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.FriendshipStatus;
import br.ufpb.dcx.apps4society.quizapi.repository.FriendshipRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.FriendshipNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotFoundException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import br.ufpb.dcx.apps4society.quizapi.util.Messages;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public FriendshipService(FriendshipRepository friendshipRepository, UserRepository userRepository, UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public FriendshipResponse requestFriendship(UUID targetId, String token) {
        User requester = userService.findUserByToken(token);
        User addressee = userRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND));

        if (requester.getUuid().equals(addressee.getUuid())) {
            throw new IllegalArgumentException("Você não pode adicionar a si mesmo");
        }

        Friendship existing = friendshipRepository.findBetween(requester, addressee).orElse(null);
        if (existing != null) {
            return existing.entityToResponse();
        }

        Friendship friendship = new Friendship(requester, addressee);
        friendshipRepository.save(friendship);
        return friendship.entityToResponse();
    }

    public FriendshipResponse acceptFriendship(Long friendshipId, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipNotFoundException("Solicitação não encontrada"));

        if (!friendship.getAddressee().getUuid().equals(user.getUuid())) {
            throw new UserNotHavePermissionException("Você não pode aceitar essa solicitação");
        }

        friendship.accept();
        friendshipRepository.save(friendship);
        return friendship.entityToResponse();
    }

    public void removeFriendship(Long friendshipId, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new FriendshipNotFoundException("Solicitação não encontrada"));

        if (!friendship.involves(user)) {
            throw new UserNotHavePermissionException("Você não tem permissão para remover essa amizade");
        }

        friendshipRepository.delete(friendship);
    }

    public List<FriendshipResponse> findMyFriends(String token) {
        User user = userService.findUserByToken(token);
        return friendshipRepository.findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED)
                .stream().map(Friendship::entityToResponse).toList();
    }

    public List<FriendshipResponse> findPendingRequests(String token) {
        User user = userService.findUserByToken(token);
        return friendshipRepository.findPendingForAddressee(user)
                .stream().map(Friendship::entityToResponse).toList();
    }
}
