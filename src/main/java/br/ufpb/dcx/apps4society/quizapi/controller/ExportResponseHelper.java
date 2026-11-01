package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseStatisticDTO;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.service.export.ExportFormat;
import br.ufpb.dcx.apps4society.quizapi.service.export.ExportService;
import br.ufpb.dcx.apps4society.quizapi.service.export.ExportTable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExportResponseHelper {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter ROW_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ExportService exportService;

    public ExportResponseHelper(ExportService exportService) {
        this.exportService = exportService;
    }

    public ExportTable buildMyResponsesTable(List<ResponseDTO> responses) {
        List<String> headers = List.of("Tema", "Questão", "Respondeu", "Data", "Acertou");
        List<List<String>> rows = responses.stream()
                .map(r -> List.of(
                        r.question().theme() != null ? r.question().theme().name() : "",
                        r.question().title(),
                        r.alternative().text(),
                        r.dateTime() != null ? r.dateTime().format(ROW_DATE) : "",
                        Boolean.TRUE.equals(r.alternative().correct()) ? "Sim" : "Não"
                ))
                .toList();
        return new ExportTable("Minhas Respostas", headers, rows);
    }

    public ExportTable buildResponsesPanelTable(List<ResponseDTO> responses) {
        List<String> headers = List.of("Usuário", "Tema", "Questão", "Respondeu", "Data", "Acertou");
        List<List<String>> rows = responses.stream()
                .map(r -> List.of(
                        r.user() != null ? r.user().name() : "",
                        r.question().theme() != null ? r.question().theme().name() : "",
                        r.question().title(),
                        r.alternative().text(),
                        r.dateTime() != null ? r.dateTime().format(ROW_DATE) : "",
                        Boolean.TRUE.equals(r.alternative().correct()) ? "Sim" : "Não"
                ))
                .toList();
        return new ExportTable("Painel de Respostas", headers, rows);
    }

    public ExportTable buildStatisticPerQuestionTable(List<ResponseStatisticDTO> statistics) {
        List<String> headers = List.of("Título da Questão", "Total de Respostas", "Respostas Certas",
                "Respostas Erradas", "% Certas", "% Erradas");
        List<List<String>> rows = statistics.stream()
                .map(s -> List.of(
                        s.getQuestionTitle(),
                        String.valueOf(s.getTotalOfAnswers()),
                        String.valueOf(s.getTotalOfCorrectAnswers()),
                        String.valueOf(s.getTotalOfIncorrectAnswers()),
                        String.format("%.1f%%", s.getPercentageOfAnswersCorrect()),
                        String.format("%.1f%%", s.getPercentageOfAnswersIncorrect())
                ))
                .toList();
        return new ExportTable("Estatísticas por Questão", headers, rows);
    }

    public ExportTable buildStatisticConclusionTable(List<StatisticResponse> statistics) {
        List<String> headers = List.of("Usuário", "Tema", "Data", "Modo", "% de Acertos");
        List<List<String>> rows = statistics.stream()
                .map(s -> List.of(
                        s.studentName(),
                        s.themeName(),
                        s.date() != null ? s.date().format(ROW_DATE) : "",
                        s.gameMode() != null && s.gameMode().name().equals("MULTIPLAYER") ? "Multiplayer" : "Um jogador",
                        s.percentagemOfHits() != null ? String.format("%.1f%%", s.percentagemOfHits()) : ""
                ))
                .toList();
        return new ExportTable("Estatísticas por Conclusão", headers, rows);
    }

    public ResponseEntity<byte[]> build(ExportFormat format, String fileBaseName, ExportTable table) {
        byte[] content;
        MediaType mediaType;
        String extension;

        if (format == ExportFormat.XLSX) {
            content = exportService.generateXlsx(table);
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            extension = "xlsx";
        } else {
            content = exportService.generatePdf(table);
            mediaType = MediaType.APPLICATION_PDF;
            extension = "pdf";
        }

        String filename = fileBaseName + "_" + LocalDateTime.now().format(FILE_TIMESTAMP) + "." + extension;

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }
}
