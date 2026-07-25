package br.ufpb.dcx.apps4society.quizapi.dto.feedback;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ThemeFeedbackResponse(
        Long id,
        Long themeId,
        UUID userUuid,
        String userName,
        String text,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime createdAt) {}
