package br.rafaeros.fastrelax_api.features.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Remove tokens que já não servem para nada.
 *
 * <p>
 * Duas tabelas, uma varredura: cada login e cada renovação criam um refresh
 * token, e cada convite ou recuperação cria um token de credencial. Sem limpeza,
 * ambas cresceriam para sempre e o índice de busca por hash degradaria.
 *
 * <p>
 * Os tokens de credencial importam por outra razão também: enquanto a linha
 * existe, ela guarda o hash de um link que já circulou por e-mail. Descartar o
 * que venceu é higiene, não só espaço.
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenService refreshTokenService;
    private final CredentialTokenService credentialTokenService;

    @Scheduled(cron = "${app.security.refresh-token.cleanup-cron:0 0 3 * * *}")
    public void purge() {
        int refreshRemoved = refreshTokenService.purgeExpired();
        int credentialRemoved = credentialTokenService.purgeExpired();

        if (refreshRemoved > 0 || credentialRemoved > 0) {
            log.info("Limpeza de tokens: {} de sessão, {} de credencial",
                    refreshRemoved, credentialRemoved);
        }
    }
}
