package br.ufpb.dcx.apps4society.quizapi.game.dto;

import java.util.List;

/** Estado completo do torneio (bracket + jogadores), consultado via polling pelo frontend. */
public record TournamentStateResponse(
        String code,
        String hostId,
        String name,
        Long themeId,
        String themeName,
        String status,
        List<PlayerView> players,
        List<List<MatchView>> rounds,
        String championId
) {
    public record PlayerView(String id, String name, boolean host, boolean eliminated,
                             String title, String frame, String banner) {}

    public record MatchView(
            String id,
            String player1Id,
            String player2Id,
            String winnerId,
            String roomCode,
            String status
    ) {}
}
