package br.ufpb.dcx.apps4society.quizapi.dto.question;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record QuestionUpdate(
        @NotBlank(message = "Campo de titulo não pode ser vazio")
        @Size(min = 4 ,max = 1500, message = "Número de caracteres inválido")
        String title,
        @Size(max = 255, message = "Número de caracteres inválido")
        @URL(message = "URL inválida")
        String imageUrl,
        @Size(max = 2_800_000, message = "Imagem excede o tamanho máximo permitido")
        String imageBase64One,
        @Size(max = 2_800_000, message = "Imagem excede o tamanho máximo permitido")
        String imageBase64Two,
        @Size(max = 50, message = "Número de caracteres inválido")
        String imagesOrder) {
}
