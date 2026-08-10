package br.rafaeros.fastrelax_api.core.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CryptoServiceTest {

    private final CryptoService cryptoService = new CryptoService("segredo-de-teste-nao-usar-em-producao");
    private static final String CPF = "52998224725";

    @Test
    @DisplayName("decrypt desfaz encrypt")
    void encryptRoundTrip() {
        assertThat(cryptoService.decrypt(cryptoService.encrypt(CPF))).isEqualTo(CPF);
    }

    @Test
    @DisplayName("mesmo valor gera ciphertexts distintos — o IV é aleatório")
    void encryptIsNotDeterministic() {
        assertThat(cryptoService.encrypt(CPF)).isNotEqualTo(cryptoService.encrypt(CPF));
    }

    @Test
    @DisplayName("blind index é determinístico: é o que sustenta a unicidade do CPF")
    void blindIndexIsDeterministic() {
        assertThat(cryptoService.blindIndex(CPF)).isEqualTo(cryptoService.blindIndex(CPF));
    }

    @Test
    @DisplayName("CPFs diferentes geram digests diferentes")
    void blindIndexDiffersPerValue() {
        assertThat(cryptoService.blindIndex(CPF)).isNotEqualTo(cryptoService.blindIndex("11144477735"));
    }

    @Test
    @DisplayName("chave diferente produz blind index diferente — trocar o segredo invalida os dados")
    void blindIndexDependsOnSecret() {
        CryptoService other = new CryptoService("outro-segredo-completamente-diferente");
        assertThat(cryptoService.blindIndex(CPF)).isNotEqualTo(other.blindIndex(CPF));
    }

    @Test
    @DisplayName("payload adulterado é rejeitado pela tag do GCM")
    void decryptRejectsTamperedPayload() {
        String encrypted = cryptoService.encrypt(CPF);
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";
        assertThatThrownBy(() -> cryptoService.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("recusa subir sem o segredo configurado")
    void requiresSecret() {
        assertThatThrownBy(() -> new CryptoService("  ")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("nulo atravessa sem quebrar")
    void handlesNull() {
        assertThat(cryptoService.encrypt(null)).isNull();
        assertThat(cryptoService.decrypt(null)).isNull();
        assertThat(cryptoService.blindIndex(null)).isNull();
    }
}
