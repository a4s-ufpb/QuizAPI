package br.ufpb.dcx.apps4society.quizapi.game.dto.ws;

import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;

/**
 * Payloads STOMP recebidos em {@code /app/game/*}. Agrupados como records
 * aninhados para manter um arquivo por conceito.
 */
public final class GameMessages {
    private GameMessages() {}

    public record Join(String code, String playerId, String name, String avatar) {}

    public record PlayerRef(String code, String playerId) {}

    public record Ready(String code, String playerId, boolean ready) {}

    public record TeamPick(String code, String playerId, String teamId) {}

    public record TeamCreate(String code, String playerId, String teamName) {}

    public record SetAvatar(String code, String playerId, String avatar) {}

    public record SetTeamAvatar(String code, String playerId, String teamId, String avatar) {}

    public record TransferCaptain(String code, String playerId, String teamId, String newCaptainId) {}

    public record Kick(String code, String hostId, String targetId) {}

    public record ChangeQuiz(String code, String hostId, Long themeId) {}

    public record ConfigUpdate(String code, String hostId, GameConfig config) {}

    public record Chat(String code, String playerId, String content) {}

    public record HostAction(String code, String hostId) {}

    public record Answer(String code, String playerId, Long alternativeId) {}
}
