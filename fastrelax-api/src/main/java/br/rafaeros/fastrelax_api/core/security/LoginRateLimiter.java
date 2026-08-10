package br.rafaeros.fastrelax_api.core.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Limite de tentativas de login por origem.
 *
 * <p>
 * O login do colaborador usa só o CPF, sem senha — sem limite, varrer CPFs até
 * achar um funcionário válido é trivial. Isto não substitui um segundo fator,
 * apenas torna a varredura inviável.
 *
 * <p>
 * O estado é em memória: suficiente para instância única, mas com várias
 * réplicas cada uma conta em separado. Numa escala assim, troque por Redis.
 */
@Component
public class LoginRateLimiter {

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Value("${app.security.login.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.security.login.window-minutes:5}")
    private int windowMinutes;

    /** Conta a tentativa e recusa quando o teto da janela é atingido. */
    public void checkAndRegister(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        Attempt attempt = attempts.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired(now, windowMinutes)) {
                return new Attempt(now);
            }
            current.count().incrementAndGet();
            return current;
        });

        if (attempt.count().get() > maxAttempts) {
            throw new BusinessException(
                    "Muitas tentativas de login. Aguarde " + windowMinutes + " minutos e tente novamente.");
        }
    }

    /** Login bem-sucedido zera o contador daquela origem. */
    public void reset(String key) {
        if (key != null && !key.isBlank()) {
            attempts.remove(key);
        }
    }

    /** Sem isto o mapa cresceria indefinidamente com chaves de janelas vencidas. */
    @Scheduled(fixedDelayString = "${app.security.login.cleanup-interval-ms:600000}")
    public void evictExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMinutes));
    }

    private record Attempt(Instant startedAt, AtomicInteger count) {
        Attempt(Instant startedAt) {
            this(startedAt, new AtomicInteger(1));
        }

        boolean isExpired(Instant now, int windowMinutes) {
            return startedAt.plus(Duration.ofMinutes(windowMinutes)).isBefore(now);
        }
    }
}
