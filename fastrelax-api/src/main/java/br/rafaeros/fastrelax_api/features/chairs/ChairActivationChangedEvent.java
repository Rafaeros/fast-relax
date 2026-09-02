package br.rafaeros.fastrelax_api.features.chairs;

/**
 * A Physical ativou ou desativou uma cadeira.
 *
 * <p>
 * {@code ChairService} não conhece sessão — só publica o fato. Quem reage
 * (encerrar uma sessão em andamento na cadeira desativada) vive no pacote de
 * colaboradores, do mesmo jeito que {@code SessionLifecycleEvent} mantém
 * sessão sem conhecer notificação. Evita a dependência circular que existiria
 * se {@code ChairService} chamasse {@code CollaboratorSessionService}
 * diretamente.
 */
public record ChairActivationChangedEvent(Long chairId, boolean active) {
}
