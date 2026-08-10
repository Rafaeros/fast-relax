package br.rafaeros.fastrelax_api.features.collaborators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Rede de fundo da expiração: as leituras já reavaliam sob demanda, então este
 * job existe para fechar sessões abandonadas quando ninguém está consultando a
 * API — caso contrário elas continuariam ativas para o índice
 * {@code uq_collaborator_active_session}, bloqueando o colaborador.
 */
@Component
@RequiredArgsConstructor
public class SessionExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(SessionExpirationJob.class);

    private final SessionExpirationService sessionExpirationService;

    @Scheduled(fixedDelayString = "${app.sessions.expiration-interval-ms:30000}")
    public void expireAbandonedSessions() {
        int expired = sessionExpirationService.expireAbandonedSessions();
        if (expired > 0) {
            log.info("Sessões expiradas automaticamente: {}", expired);
        }
    }
}
