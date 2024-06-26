package br.ufpb.dcx.apps4society.quizapi.dto.openai;

public record GPTMessage(
        String role,
        String content
) {
}
