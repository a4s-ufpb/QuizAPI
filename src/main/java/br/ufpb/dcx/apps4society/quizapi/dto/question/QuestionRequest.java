package br.ufpb.dcx.apps4society.quizapi.dto.question;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record QuestionRequest(
        @NotBlank(message = "Campo de titulo não pode ser vazio")
        @Size(min = 4 ,max = 1500, message = "Número de caracteres inválido")
        String title,
        @Size(max = 255, message = "Número de caracteres inválido")
        @URL(message = "URL inválida")
        String imageUrl,
        // Base64 (upload novo) ou URL já armazenada no MinIO (edição sem trocar imagem).
        @Size(max = 2_800_000, message = "Imagem excede o tamanho máximo permitido")
        String imageOneUrl,
        @Size(max = 2_800_000, message = "Imagem excede o tamanho máximo permitido")
        String imageTwoUrl,
        @Size(max = 50, message = "Número de caracteres inválido")
        String imagesOrder) {
}
