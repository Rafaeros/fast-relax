package br.rafaeros.fastrelax_api.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

class PhoneUtilsTest {

    @Test
    @DisplayName("remove a máscara vinda da planilha do RH")
    void normalizeStripsMask() {
        assertThat(PhoneUtils.normalize("(43) 98412-8306")).isEqualTo("43984128306");
        assertThat(PhoneUtils.normalize("+55 (43) 98412-8306")).isEqualTo("5543984128306");
    }

    @Test
    @DisplayName("mantém telefone já em dígitos")
    void normalizeKeepsDigits() {
        assertThat(PhoneUtils.normalize("5543995997676")).isEqualTo("5543995997676");
    }

    @Test
    @DisplayName("rejeita telefone curto ou longo demais")
    void normalizeRejectsOutOfRange() {
        assertThatThrownBy(() -> PhoneUtils.normalize("123456789")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PhoneUtils.normalize("12345678901234")).isInstanceOf(BusinessException.class);
    }
}
