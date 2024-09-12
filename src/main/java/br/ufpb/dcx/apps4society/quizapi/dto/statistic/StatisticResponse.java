package br.ufpb.dcx.apps4society.quizapi.dto.statistic;

import java.time.LocalDate;
import java.util.UUID;

public record StatisticResponse(
         Long id,
         UUID creatorId,
         String studentName,
         String themeName,
         Double percentagemOfHits,
         LocalDate date
) {
}
