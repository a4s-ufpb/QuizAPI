package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThemeUpdate(
        @NotBlank(message = "Campo tema não pode ser vazio")
        @Size(min = 3, max = 70, message = "Número de caracteres inválido")
        String name,
        // Aceita tanto uma URL já hospedada quanto uma imagem em base64
        // (upload novo, convertido pra webp e enviado ao MinIO no service).
        String imageUrl) {
}
