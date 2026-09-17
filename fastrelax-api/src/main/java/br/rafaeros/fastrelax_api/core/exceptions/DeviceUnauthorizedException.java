package br.rafaeros.fastrelax_api.core.exceptions;

/**
 * Token do dispositivo ausente ou divergente do que está pareado com a
 * cadeira. Separado de {@link org.springframework.security.core.AuthenticationException}
 * porque aquele handler devolve uma mensagem fixa pensada para login de
 * usuário ("Email ou senha incorretos") — aqui quem lê é firmware, e a
 * mensagem real ajuda a diagnosticar cadeira mal pareada.
 */
public class DeviceUnauthorizedException extends RuntimeException {
    public DeviceUnauthorizedException(String message) {
        super(message);
    }
}
