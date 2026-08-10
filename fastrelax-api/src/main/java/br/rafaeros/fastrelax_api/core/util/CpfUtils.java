package br.rafaeros.fastrelax_api.core.util;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Normalização de CPF compartilhada entre o cadastro individual e a importação
 * em massa.
 *
 * <p>
 * Centralizar isto importa para a segurança do blind index: se dois caminhos
 * normalizassem de formas diferentes, o mesmo CPF geraria digests distintos e a
 * constraint de unicidade deixaria de valer.
 */
public final class CpfUtils {

    private static final int CPF_LENGTH = 11;

    private CpfUtils() {
    }

    /** Remove qualquer formatação, exige os 11 dígitos e confere os verificadores. */
    public static String normalize(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new BusinessException("CPF é obrigatório");
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != CPF_LENGTH) {
            throw new BusinessException("CPF deve conter 11 dígitos");
        }
        if (!hasValidCheckDigits(digits)) {
            throw new BusinessException("CPF inválido");
        }
        return digits;
    }

    /**
     * Verificação dos dois dígitos finais (módulo 11).
     *
     * <p>
     * Vale a pena mesmo sendo o CPF a credencial de login: sem isto, um dígito
     * trocado no cadastro cria um colaborador que nunca consegue entrar, e a
     * correção exige mexer no banco.
     */
    public static boolean hasValidCheckDigits(String digits) {
        if (digits == null || digits.length() != CPF_LENGTH) {
            return false;
        }
        // Sequências repetidas (00000000000, 11111111111...) satisfazem o cálculo
        // por acidente, então são descartadas antes.
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == Character.getNumericValue(digits.charAt(9))
                && checkDigit(digits, 10) == Character.getNumericValue(digits.charAt(10));
    }

    private static int checkDigit(String digits, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int index = 0; index < position; index++) {
            sum += Character.getNumericValue(digits.charAt(index)) * weight--;
        }
        int remainder = sum % CPF_LENGTH;
        return remainder < 2 ? 0 : CPF_LENGTH - remainder;
    }

    /**
     * Reaplica os zeros à esquerda que o Excel remove ao tratar o CPF como número.
     * "1234567890" (10 dígitos) vira "01234567890".
     */
    public static String padLeadingZeros(String digits) {
        if (digits == null || digits.length() >= CPF_LENGTH) {
            return digits;
        }
        return "0".repeat(CPF_LENGTH - digits.length()) + digits;
    }
}
