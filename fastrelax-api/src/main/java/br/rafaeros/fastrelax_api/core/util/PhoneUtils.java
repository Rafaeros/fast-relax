package br.rafaeros.fastrelax_api.core.util;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Normalização de telefone, pela mesma razão do CPF: guardar só dígitos e deixar
 * a máscara para o frontend.
 *
 * <p>
 * Sem isto o banco acumularia "(43) 98412-8306" vindo de planilha e
 * "5543995997676" vindo do cadastro individual, e qualquer busca por telefone
 * dependeria de qual caminho criou o registro.
 */
public final class PhoneUtils {

    /** DDD + 8 dígitos. */
    private static final int MIN_DIGITS = 10;
    /** DDI + DDD + 9 dígitos. */
    private static final int MAX_DIGITS = 13;

    private PhoneUtils() {
    }

    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Telefone é obrigatório");
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
            throw new BusinessException(
                    "Telefone deve conter entre " + MIN_DIGITS + " e " + MAX_DIGITS + " dígitos");
        }
        return digits;
    }
}
