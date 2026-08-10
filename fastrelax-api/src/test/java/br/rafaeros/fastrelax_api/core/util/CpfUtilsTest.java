package br.rafaeros.fastrelax_api.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

class CpfUtilsTest {

    /** CPF válido gerado para teste (dígitos verificadores corretos). */
    private static final String VALID_CPF = "52998224725";

    @ParameterizedTest
    @DisplayName("aceita CPF válido com e sem máscara, sempre devolvendo só dígitos")
    @ValueSource(strings = { "52998224725", "529.982.247-25", "529 982 247 25" })
    void normalizeAcceptsMaskedAndUnmasked(String input) {
        assertThat(CpfUtils.normalize(input)).isEqualTo(VALID_CPF);
    }

    @Test
    @DisplayName("rejeita CPF com dígito verificador errado")
    void normalizeRejectsWrongCheckDigits() {
        assertThatThrownBy(() -> CpfUtils.normalize("52998224726"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CPF inválido");
    }

    @ParameterizedTest
    @DisplayName("rejeita sequências repetidas, que passariam no cálculo do módulo 11")
    @ValueSource(strings = { "00000000000", "11111111111", "99999999999" })
    void normalizeRejectsRepeatedSequences(String input) {
        assertThatThrownBy(() -> CpfUtils.normalize(input)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("rejeita quantidade de dígitos diferente de 11")
    void normalizeRejectsWrongLength() {
        assertThatThrownBy(() -> CpfUtils.normalize("1234567890"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CPF deve conter 11 dígitos");
    }

    @Test
    @DisplayName("exige CPF preenchido")
    void normalizeRejectsBlank() {
        assertThatThrownBy(() -> CpfUtils.normalize("  "))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CPF é obrigatório");
    }

    @Test
    @DisplayName("repõe os zeros à esquerda que o Excel remove ao tratar CPF como número")
    void padLeadingZerosRestoresExcelTruncation() {
        assertThat(CpfUtils.padLeadingZeros("1234567890")).isEqualTo("01234567890");
        assertThat(CpfUtils.padLeadingZeros("52998224725")).isEqualTo(VALID_CPF);
    }
}
