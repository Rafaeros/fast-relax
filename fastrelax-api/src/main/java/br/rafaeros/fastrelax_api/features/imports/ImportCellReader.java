package br.rafaeros.fastrelax_api.features.imports;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Leitura tolerante de células.
 *
 * <p>
 * O Excel não preserva o tipo que o usuário "quis" digitar: CPF vira número e
 * perde zeros à esquerda, telefone vira notação científica e horário vira fração
 * do dia. Cada método aqui existe para desfazer uma dessas conversões.
 */
final class ImportCellReader {

    private ImportCellReader() {
    }

    /** Texto puro, sem notação científica quando a célula é numérica. */
    static String readString(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            // BigDecimal.toPlainString evita "5.5439959976E10" em telefones e CPFs.
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> readFormulaAsString(cell);
            default -> null;
        };
    }

    /**
     * Horário em qualquer das formas que o Excel produz: célula formatada como
     * hora (fração do dia), número solto ou texto "12:00" / "12:00:00".
     */
    static LocalTime readTime(Row row, int column, String fieldName) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            throw new BusinessException(fieldName + " é obrigatório");
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalTime();
            }
            // Fração do dia: 0,5 = 12:00. Arredonda para o minuto mais próximo
            // porque a representação em ponto flutuante gera 11:59:59.
            long totalMinutes = Math.round(value * 24 * 60);
            return LocalTime.of((int) (totalMinutes / 60) % 24, (int) (totalMinutes % 60));
        }
        String raw = readString(row, column);
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(fieldName + " é obrigatório");
        }
        return parseTime(raw, fieldName);
    }

    private static LocalTime parseTime(String raw, String fieldName) {
        String value = raw.trim().replace('h', ':').replace('H', ':');
        String[] parts = value.split(":");
        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 && !parts[1].isBlank() ? Integer.parseInt(parts[1].trim()) : 0;
            return LocalTime.of(hour, minute);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | java.time.DateTimeException e) {
            throw new BusinessException(fieldName + " inválido: '" + raw + "'. Use o formato HH:mm");
        }
    }

    private static String readFormulaAsString(Cell cell) {
        try {
            return cell.getStringCellValue().trim();
        } catch (IllegalStateException e) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        }
    }

    /** Verdadeiro quando a linha está totalmente vazia — comum no fim da planilha. */
    static boolean isBlank(Row row, int lastColumn) {
        for (int column = 0; column <= lastColumn; column++) {
            String value = readString(row, column);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
