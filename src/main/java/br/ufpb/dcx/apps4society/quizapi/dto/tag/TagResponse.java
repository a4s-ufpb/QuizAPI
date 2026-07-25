package br.ufpb.dcx.apps4society.quizapi.dto.tag;

import java.util.UUID;

public record TagResponse(Long id, String name, UUID creatorId) {}
