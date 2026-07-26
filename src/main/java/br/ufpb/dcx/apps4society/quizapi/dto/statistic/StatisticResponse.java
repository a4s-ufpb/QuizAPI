package br.ufpb.dcx.apps4society.quizapi.dto.statistic;

import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record StatisticResponse(
         Long id,
         UUID creatorId,
         UUID studentUuid,
         String studentName,
         String themeName,
         Double percentagemOfHits,
         @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
         LocalDateTime date,
         GameMode gameMode
) {
}
