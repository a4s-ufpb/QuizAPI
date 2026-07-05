package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.game.dto.ws.GameMessages.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Destinos STOMP do modo multiplayer, prefixo {@code /app/game/*}.
 * Cada handler delega ao {@link GameRoomService}, que faz o broadcast do
 * resultado para {@code /topic/room/{code}}.
 */
@Controller
public class GameWebSocketController {

    private final GameRoomService gameRoomService;

    public GameWebSocketController(GameRoomService gameRoomService) {
        this.gameRoomService = gameRoomService;
    }

    @MessageMapping("/game/join")
    public void join(Join msg) {
        gameRoomService.join(msg.code(), msg.playerId(), msg.name(), msg.avatar(), msg.userUuid());
    }

    @MessageMapping("/game/leave")
    public void leave(PlayerRef msg) {
        gameRoomService.leave(msg.code(), msg.playerId());
    }

    @MessageMapping("/game/ready")
    public void ready(Ready msg) {
        gameRoomService.setReady(msg.code(), msg.playerId(), msg.ready());
    }

    @MessageMapping("/game/team")
    public void team(TeamPick msg) {
        gameRoomService.pickTeam(msg.code(), msg.playerId(), msg.teamId());
    }

    @MessageMapping("/game/team/create")
    public void createTeam(TeamCreate msg) {
        gameRoomService.createTeam(msg.code(), msg.playerId(), msg.teamName());
    }

    @MessageMapping("/game/avatar")
    public void setAvatar(SetAvatar msg) {
        gameRoomService.setAvatar(msg.code(), msg.playerId(), msg.avatar());
    }

    @MessageMapping("/game/team/avatar")
    public void setTeamAvatar(SetTeamAvatar msg) {
        gameRoomService.setTeamAvatar(msg.code(), msg.playerId(), msg.teamId(), msg.avatar());
    }

    @MessageMapping("/game/team/captain")
    public void transferCaptain(TransferCaptain msg) {
        gameRoomService.transferCaptain(msg.code(), msg.playerId(), msg.teamId(), msg.newCaptainId());
    }

    @MessageMapping("/game/kick")
    public void kick(Kick msg) {
        gameRoomService.kick(msg.code(), msg.hostId(), msg.targetId());
    }

    @MessageMapping("/game/change-quiz")
    public void changeQuiz(ChangeQuiz msg) {
        gameRoomService.changeQuiz(msg.code(), msg.hostId(), msg.themeId());
    }

    @MessageMapping("/game/config")
    public void config(ConfigUpdate msg) {
        gameRoomService.updateConfig(msg.code(), msg.hostId(), msg.config());
    }

    @MessageMapping("/game/power")
    public void setPower(SetPower msg) {
        gameRoomService.setPendingPower(msg.code(), msg.hostId(), msg.power());
    }

    @MessageMapping("/game/chat")
    public void chat(Chat msg) {
        gameRoomService.chat(msg.code(), msg.playerId(), msg.content());
    }

    @MessageMapping("/game/start")
    public void start(HostAction msg) {
        gameRoomService.start(msg.code(), msg.hostId());
    }

    @MessageMapping("/game/answer")
    public void answer(Answer msg) {
        gameRoomService.answer(msg.code(), msg.playerId(), msg.alternativeId());
    }

    @MessageMapping("/game/next")
    public void next(HostAction msg) {
        gameRoomService.next(msg.code(), msg.hostId());
    }
}
