package br.ufpb.dcx.apps4society.quizapi.dto.message;

public record QuizMessage(
         String type,
         String content,
         String sender
) {
}
