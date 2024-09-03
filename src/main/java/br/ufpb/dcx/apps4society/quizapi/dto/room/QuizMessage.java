package br.ufpb.dcx.apps4society.quizapi.dto.room;

public record QuizMessage(
         String type,
         String content,
         String sender
) {
}
