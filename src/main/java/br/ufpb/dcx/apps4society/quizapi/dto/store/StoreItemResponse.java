package br.ufpb.dcx.apps4society.quizapi.dto.store;

public record StoreItemResponse(
        String code,
        String name,
        String description,
        String category,
        int price,
        boolean owned,
        /** Nível mínimo do usuário para desbloquear a compra deste item. */
        int requiredLevel
) {
}
