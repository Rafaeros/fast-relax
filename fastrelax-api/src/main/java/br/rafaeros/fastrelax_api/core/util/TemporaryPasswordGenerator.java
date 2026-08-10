package br.rafaeros.fastrelax_api.core.util;

import java.security.SecureRandom;

/**
 * Gera a senha temporária de primeiro acesso.
 *
 * <p>
 * O alfabeto exclui caracteres que se confundem quando a senha é ditada ou
 * copiada à mão — {@code 0/O}, {@code 1/l/I} — porque na prática o ADMIN vai
 * repassá-la por mensagem ou verbalmente.
 */
public final class TemporaryPasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int index = 0; index < LENGTH; index++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
