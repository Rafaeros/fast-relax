package br.rafaeros.fastrelax_api.features.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Remove refresh tokens vencidos ou já revogados.
 *
 * <p>
 * Cada login e cada renovação criam uma linha; sem limpeza, a tabela cresceria
 * para sempre e o índice de busca por hash degradaria.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "${app.security.refresh-token.cleanup-cron:0 0 3 * * *}")
    public void purge() {
        int removed = refreshTokenService.purgeExpired();
        if (removed > 0) {
            log.info("Refresh tokens removidos na limpeza: {}", removed);
        }
    }
}
