package br.ufpb.dcx.apps4society.quizapi.dto.openai;

public record Choice(
        Integer index,
        GPTMessage message
) {
}
