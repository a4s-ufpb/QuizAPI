package br.ufpb.dcx.apps4society.quizapi.game.dto;

/** Mensagem de chat da sala (efêmera, não persistida). */
public record ChatMessage(
        String playerId,
        String name,
        String content,
        long timestamp
) {}
