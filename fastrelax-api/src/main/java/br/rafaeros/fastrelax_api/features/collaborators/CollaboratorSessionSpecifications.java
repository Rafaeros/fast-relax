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

    public static Specification<CollaboratorSession> hasCollaborator(Long collaboratorId) {
        return (root, query, cb) -> collaboratorId == null
                ? null
                : cb.equal(root.get("collaborator").get("id"), collaboratorId);
    }
}
