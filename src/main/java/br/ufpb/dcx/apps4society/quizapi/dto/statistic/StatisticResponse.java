package br.ufpb.dcx.apps4society.quizapi.dto.statistic;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;

public record StatisticResponse(
         Long id,
         UUID creatorId,
         String studentName,
         String themeName,
         Double percentagemOfHits,
         @JsonFormat(pattern = "dd/MM/yyyy")
         LocalDate date
) {
}
