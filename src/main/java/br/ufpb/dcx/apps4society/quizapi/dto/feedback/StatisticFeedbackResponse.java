package br.ufpb.dcx.apps4society.quizapi.dto.feedback;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatisticFeedbackResponse(
        Long id,
        Long statisticId,
        String themeName,
        String studentName,
        UUID creatorUuid,
        String creatorName,
        String text,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime createdAt) {}
