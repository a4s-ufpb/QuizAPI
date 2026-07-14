package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ThemeRequest(
        @NotBlank(message = "Campo tema não pode ser vazio")
        @Size(min = 3, max = 70, message = "Número de caracteres inválido")
        String name,
        // Aceita tanto uma URL já hospedada quanto uma imagem em base64
        // (upload novo, convertido pra webp e enviado ao MinIO no service).
        String imageUrl,
        /** Conteúdos abordados pelo tema (texto livre, opcional). */
        String description,
        /** Materiais de apoio associados ao tema (opcional). */
        @Valid
        List<MaterialRequest> materials) {

    // Compat: criação simples só com nome + imagem.
    public ThemeRequest(String name, String imageUrl) {
        this(name, imageUrl, null, List.of());
    }

    public List<MaterialRequest> materialsOrEmpty() {
        return materials != null ? materials : List.of();
    }
}
