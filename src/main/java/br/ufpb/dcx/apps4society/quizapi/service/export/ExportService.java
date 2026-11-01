package br.ufpb.dcx.apps4society.quizapi.service.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ExportService {
    private static final int COLUMN_PADDING_CHARS = 2;
    private static final int MAX_COLUMN_WIDTH_CHARS = 80;
    private static final int EXCEL_WIDTH_UNITS_PER_CHAR = 256;

    public byte[] generateXlsx(ExportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(table.title());

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                var rowData = table.rows().get(rowIndex);
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    row.createCell(colIndex).setCellValue(rowData.get(colIndex));
                }
            }

            // autoSizeColumn mede texto via AWT, que exige fontconfig e fontes
            // ausentes na imagem Alpine de produção.
            for (int i = 0; i < table.headers().size(); i++) {
                sheet.setColumnWidth(i, columnWidth(table, i));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExportGenerationException("Erro ao gerar arquivo Excel", e);
        }
    }

    private int columnWidth(ExportTable table, int column) {
        int longest = table.headers().get(column).length();
        for (var row : table.rows()) {
            longest = Math.max(longest, row.get(column).length());
        }
        return Math.min(longest + COLUMN_PADDING_CHARS, MAX_COLUMN_WIDTH_CHARS) * EXCEL_WIDTH_UNITS_PER_CHAR;
    }

    public byte[] generatePdf(ExportTable table) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(table.title(), new Font(Font.HELVETICA, 16, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(16);
            document.add(title);

            PdfPTable pdfTable = new PdfPTable(table.headers().size());
            pdfTable.setWidthPercentage(100);

            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            for (String header : table.headers()) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                pdfTable.addCell(cell);
            }

            Font bodyFont = new Font(Font.HELVETICA, 9);
            for (var rowData : table.rows()) {
                for (String value : rowData) {
                    pdfTable.addCell(new PdfPCell(new Phrase(value == null ? "" : value, bodyFont)));
                }
            }

            document.add(pdfTable);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new ExportGenerationException("Erro ao gerar arquivo PDF", e);
        }
    }
}
