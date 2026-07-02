package br.ufpb.dcx.apps4society.quizapi.game.dto;

/** Corpo do POST que cria uma sala. {@code config} é opcional. */
public record CreateRoomRequest(
        String hostId,
        String hostName,
        Long themeId,
        GameConfig config
) {}
