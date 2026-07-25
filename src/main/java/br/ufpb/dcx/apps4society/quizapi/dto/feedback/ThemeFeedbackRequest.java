package br.ufpb.dcx.apps4society.quizapi.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThemeFeedbackRequest(
        @NotBlank(message = "Feedback não pode ser vazio")
        @Size(min = 5, max = 2000, message = "O feedback deve ter no mínimo 5 caracteres")
        String text
) {}
