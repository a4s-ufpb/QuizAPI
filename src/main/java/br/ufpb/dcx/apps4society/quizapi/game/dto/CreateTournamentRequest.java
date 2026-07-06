package br.ufpb.dcx.apps4society.quizapi.game.dto;

/** Corpo do POST que cria um torneio. */
public record CreateTournamentRequest(
        String hostId,
        String hostName,
        String name,
        Long themeId,
        Integer questionCount,
        Integer questionTimeSeconds,
        Integer maxPlayers
) {}
