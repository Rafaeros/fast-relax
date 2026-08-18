package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * O que aconteceu com uma sessão, publicado para quem quiser reagir.
 *
 * <p>
 * As regras de sessão não conhecem notificação: elas anunciam o fato e seguem.
 * Quem avisa o colaborador é o listener no pacote de notificações. Sem essa
 * separação, acrescentar e-mail ou WhatsApp amanhã exigiria mexer no serviço que
 * agenda massagem — código que não tem nada a ver com entrega de mensagem.
 *
 * <p>
 * Carrega valores, não a entidade: o listener roda depois do commit, em outra
 * transação, onde uma referência lazy já estaria desanexada.
 */
public record SessionLifecycleEvent(
        Type type,
        Long sessionId,
        Long collaboratorId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime) {

    public enum Type {
        SCHEDULED,
        STARTED,
        FINISHED,
        EXPIRED,
        CANCELLED
    }

    public static SessionLifecycleEvent of(Type type, CollaboratorSession session) {
        return new SessionLifecycleEvent(
                type,
                session.getId(),
                session.getCollaborator().getId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime());
    }
}
