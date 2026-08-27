package br.rafaeros.fastrelax_api.core.util;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Normalização de CNPJ, pelo mesmo motivo do CPF: o banco guarda só dígitos.
 *
 * <p>
 * Aqui isso é mais que arrumação de dados. O CNPJ é como o colaborador diz de
 * qual empresa é na tela de login, e a busca é por igualdade exata — se o
 * cadastro gravasse "12.345.678/0001-90" e o login enviasse "12345678000190",
 * ninguém entraria.
 */
public final class CnpjUtils {

    private static final int CNPJ_LENGTH = 14;

    private CnpjUtils() {
    }

    /** Remove a formatação e exige os 14 dígitos. */
    public static String normalize(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            throw new BusinessException("CNPJ é obrigatório");
        }
        String digits = cnpj.replaceAll("\\D", "");
        if (digits.length() != CNPJ_LENGTH) {
            throw new BusinessException("CNPJ deve conter 14 dígitos");
        }
        if (!hasValidCheckDigits(digits)) {
            throw new BusinessException("CNPJ inválido");
        }
        return digits;
    }

    /**
     * Mesma normalização, sem estourar quando o valor não presta.
     *
     * <p>
     * É o que o login usa: um CNPJ malformado ali não é erro de cadastro, é
     * tentativa inválida, e precisa responder igual a uma empresa inexistente —
     * mensagem diferente para cada caso deixa enumerar quem é cliente.
     */
    public static String normalizeQuietly(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        return cnpj.replaceAll("\\D", "");
    }

    /** Verificação dos dois dígitos finais (módulo 11, pesos 2..9 cíclicos). */
    public static boolean hasValidCheckDigits(String digits) {
        if (digits == null || digits.length() != CNPJ_LENGTH) {
            return false;
        }
        // Sequências repetidas satisfazem o cálculo por acidente.
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 12) == Character.getNumericValue(digits.charAt(12))
                && checkDigit(digits, 13) == Character.getNumericValue(digits.charAt(13));
    }

    private static int checkDigit(String digits, int position) {
        int sum = 0;
        int weight = 2;
        for (int index = position - 1; index >= 0; index--) {
            sum += Character.getNumericValue(digits.charAt(index)) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
