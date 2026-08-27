package br.rafaeros.fastrelax_api.features.imports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Gera a planilha de colaboradores, usada tanto pelo modelo em branco quanto
 * pela exportação.
 *
 * <p>
 * O mesmo layout serve aos dois porque o arquivo exportado precisa poder ser
 * editado e reimportado sem ajuste manual de colunas.
 *
 * <p>
 * Todas as células saem formatadas como texto: sem isso o Excel converte CPF em
 * número e apaga zeros à esquerda assim que o arquivo é aberto.
 */
final class CollaboratorSheetBuilder {

    /**
     * E-mail entra por último de propósito: planilha antiga, sem a coluna,
     * continua sendo aceita — a linha só entra sem e-mail, e a pessoa recebe
     * senha temporária em vez de convite.
     */
    static final String[] HEADERS = {
            "Nome", "CPF", "Telefone", "Departamento", "Inicio Almoco", "Fim Almoco", "Email"
    };

    /** Exemplo com máscara: comunica que CPF e telefone podem vir formatados. */
    static final String[] SAMPLE = {
            "Renata Perez", "123.456.789-00", "(43) 98412-8306", "Recursos Humanos", "12:00", "13:00",
            "renata.perez@empresa.com"
    };

    private CollaboratorSheetBuilder() {
    }

    static byte[] build(List<String[]> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Colaboradores");

            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            // "@" é o formato de texto do Excel: preserva zeros à esquerda no CPF.
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < HEADERS.length; column++) {
                    Cell cell = row.createCell(column);
                    cell.setCellValue(column < values.length && values[column] != null ? values[column] : "");
                    cell.setCellStyle(textStyle);
                }
            }

            for (int column = 0; column < HEADERS.length; column++) {
                sheet.setColumnWidth(column, 6000);
                sheet.setDefaultColumnStyle(column, textStyle);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Não foi possível gerar a planilha");
        }
    }
}
