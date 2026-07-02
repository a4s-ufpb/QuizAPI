package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record ThemeUpdate(
        @NotBlank(message = "Campo tema não pode ser vazio")
        @Size(min = 3, max = 70, message = "Número de caracteres inválido")
        String name,
        @Size(max = 255, message = "Número de caracteres inválido")
        @URL(message = "Url inválida")
        String imageUrl) {
}
