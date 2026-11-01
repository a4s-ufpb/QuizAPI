package br.ufpb.dcx.apps4society.quizapi.service.export;

import java.util.List;

public record ExportTable(
        String title,
        List<String> headers,
        List<List<String>> rows
) {
}
