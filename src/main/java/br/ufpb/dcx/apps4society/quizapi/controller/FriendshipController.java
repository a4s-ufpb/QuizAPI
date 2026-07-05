package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.friendship.FriendshipResponse;
import br.ufpb.dcx.apps4society.quizapi.service.FriendshipService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/friendship")
@Tag(name = "Friendship", description = "Friends of Quiz A4S users")
public class FriendshipController {
    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/request/{targetId}")
    public ResponseEntity<FriendshipResponse> requestFriendship(@PathVariable UUID targetId,
                                                                  @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(friendshipService.requestFriendship(targetId, token));
    }

    @PatchMapping("/{friendshipId}/accept")
    public ResponseEntity<FriendshipResponse> acceptFriendship(@PathVariable Long friendshipId,
                                                                 @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.ok(friendshipService.acceptFriendship(friendshipId, token));
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<Void> removeFriendship(@PathVariable Long friendshipId,
                                                  @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        friendshipService.removeFriendship(friendshipId, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FriendshipResponse>> findMyFriends(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(friendshipService.findMyFriends(token));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendshipResponse>> findPendingRequests(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(friendshipService.findPendingRequests(token));
    }
}
