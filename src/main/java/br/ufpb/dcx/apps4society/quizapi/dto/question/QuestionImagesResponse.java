package br.ufpb.dcx.apps4society.quizapi.dto.question;

/**
 * Apenas as imagens de uma questão, sem título/alternativas/gabarito — usado
 * pelo modo multiplayer para buscar as imagens sob demanda (o broadcast via
 * STOMP não envia mais os base64, só metadados), sem expor a resposta certa.
 */
public record QuestionImagesResponse(
        Long id,
        String imageUrl,
        String imageBase64One,
        String imageBase64Two,
        String imagesOrder
) {
}
