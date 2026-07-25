package br.ufpb.dcx.apps4society.quizapi.dto.theme;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ThemeRequest(
        @NotBlank(message = "Campo tema não pode ser vazio")
        @Size(min = 3, max = 70, message = "Número de caracteres inválido")
        String name,
        String imageUrl,
        String description,
        @Valid
        List<MaterialRequest> materials,
        /** IDs de tags do usuário logado para associar ao tema. */
        List<Long> tagIds) {

    public ThemeRequest(String name, String imageUrl) {
        this(name, imageUrl, null, List.of(), List.of());
    }

    public ThemeRequest(String name, String imageUrl, String description, List<MaterialRequest> materials) {
        this(name, imageUrl, description, materials, List.of());
    }

    public List<MaterialRequest> materialsOrEmpty() {
        return materials != null ? materials : List.of();
    }

    public List<Long> tagIdsOrEmpty() {
        return tagIds != null ? tagIds : List.of();
    }
}
