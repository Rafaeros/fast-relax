package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.rafaeros.fastrelax_api.features.chairs.ChairActivationChangedEvent;
import lombok.RequiredArgsConstructor;

/**
 * Encerra a sessão em andamento quando a Physical desativa a cadeira dela.
 *
 * <p>
 * Vive em {@code collaborators}, não em {@code chairs}: é aqui que mora a
 * regra de sessão, e {@code ChairService} não precisa saber que sessão
 * existe — só publica o fato de que a cadeira mudou de estado. Mesmo desenho
 * de {@link SessionNotificationListener}, mas para o efeito colateral em vez
 * do aviso.
 */
@Component
@RequiredArgsConstructor
public class ChairDeactivationListener {

    private final CollaboratorSessionService sessionService;

    /**
     * Só depois do commit do toggle: reagir durante a transação do
     * {@code ChairService} arriscaria encerrar a sessão e depois o próprio
     * toggle reverter por outro motivo, deixando o colaborador sem massagem
     * numa cadeira que continuou ativa.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onChairActivationChanged(ChairActivationChangedEvent event) {
        if (event.active()) {
            return;
        }
        sessionService.forceStopByChair(event.chairId());
    }
}
