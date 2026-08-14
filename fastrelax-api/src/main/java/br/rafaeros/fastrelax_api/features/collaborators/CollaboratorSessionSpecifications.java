package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable filters for {@link CollaboratorSession} queries.
 * Each factory returns {@code null} when its argument is absent, so callers can
 * combine them with {@link Specification#allOf} and unset filters are ignored.
 */
public final class CollaboratorSessionSpecifications {

    private CollaboratorSessionSpecifications() {
    }

    public static Specification<CollaboratorSession> hasStatus(SessionStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get("status"), status);
    }

    public static Specification<CollaboratorSession> onDate(java.time.LocalDate sessionDate) {
        return (root, query, cb) -> sessionDate == null
                ? null
                : cb.equal(root.get("sessionDate"), sessionDate);
    }

    /**
     * Intervalo fechado de datas. Cada extremo é opcional: informar só um deles
     * vale como "a partir de" ou "até".
     */
    public static Specification<CollaboratorSession> betweenDates(java.time.LocalDate from,
            java.time.LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("sessionDate"), to);
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("sessionDate"), from);
            }
            return cb.between(root.get("sessionDate"), from, to);
        };
    }

    public static Specification<CollaboratorSession> hasCollaborator(Long collaboratorId) {
        return (root, query, cb) -> collaboratorId == null
                ? null
                : cb.equal(root.get("collaborator").get("id"), collaboratorId);
    }
}
