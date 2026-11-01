package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.service.export.ExportFormat;
import br.ufpb.dcx.apps4society.quizapi.service.export.ExportService;
import br.ufpb.dcx.apps4society.quizapi.service.export.ExportTable;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportResponseHelperTest {
    private static final ExportTable TABLE = new ExportTable(
            "Estatísticas por Conclusão",
            List.of("Usuário", "Tema"),
            List.of(List.of("João", "Padrões GRASP"))
    );

    private final ExportResponseHelper helper = new ExportResponseHelper(new ExportService());

    @Test
    void pdfFilenameIsPlainAndKeepsExtension() {
        ResponseEntity<byte[]> response = helper.build(ExportFormat.PDF, "estatisticas_por_conclusao", TABLE);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(disposition.matches("attachment; filename=\"estatisticas_por_conclusao_\\d{8}_\\d{6}\\.pdf\""), disposition);
        String signature = new String(Arrays.copyOf(response.getBody(), 5), StandardCharsets.US_ASCII);
        assertEquals("%PDF-", signature);
    }

    @Test
    void xlsxHasContentAndColumnsSizedToLongestValue() throws IOException {
        ResponseEntity<byte[]> response = helper.build(ExportFormat.XLSX, "estatisticas_por_conclusao", TABLE);

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposition.endsWith(".xlsx\""), disposition);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getBody()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Usuário", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Padrões GRASP", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(("Padrões GRASP".length() + 2) * 256, sheet.getColumnWidth(1));
        }
    }
}
