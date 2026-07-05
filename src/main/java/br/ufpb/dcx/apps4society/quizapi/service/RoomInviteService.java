package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.invite.RoomInviteRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.invite.RoomInviteResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Convites de sala, guardados só em memória (efêmeros, um pendente por
// usuário) — o frontend consulta via polling enquanto estiver logado.
@Service
public class RoomInviteService {
    private final Map<UUID, RoomInviteResponse> pendingInvites = new ConcurrentHashMap<>();
    private final UserService userService;

    public RoomInviteService(UserService userService) {
        this.userService = userService;
    }

    public void sendInvite(UUID targetId, RoomInviteRequest request, String token) {
        User sender = userService.findUserByToken(token);
        pendingInvites.put(targetId, new RoomInviteResponse(sender.getName(), request.roomCode()));
    }

    public RoomInviteResponse findMyInvite(String token) {
        User user = userService.findUserByToken(token);
        return pendingInvites.get(user.getUuid());
    }

    public void dismissInvite(String token) {
        User user = userService.findUserByToken(token);
        pendingInvites.remove(user.getUuid());
    }
}
