package br.ufpb.dcx.apps4society.quizapi.dto.openai;

import java.util.List;

public record ChatGptResponse(
        List<Choice> choices
) {
}
