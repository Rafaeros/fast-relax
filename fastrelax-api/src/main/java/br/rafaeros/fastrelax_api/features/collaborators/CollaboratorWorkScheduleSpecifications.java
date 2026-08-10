package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable filters for {@link CollaboratorWorkSchedule} queries.
 * Each factory returns {@code null} when its argument is absent, so callers can
 * combine them with {@link Specification#allOf} and unset filters are ignored.
 */
public final class CollaboratorWorkScheduleSpecifications {

    private CollaboratorWorkScheduleSpecifications() {
    }

    public static Specification<CollaboratorWorkSchedule> hasDayOfWeek(WorkDay dayOfWeek) {
        return (root, query, cb) -> dayOfWeek == null
                ? null
                : cb.equal(root.get("dayOfWeek"), dayOfWeek);
    }

    public static Specification<CollaboratorWorkSchedule> hasActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? null
                : cb.equal(root.get("active"), active);
    }

    public static Specification<CollaboratorWorkSchedule> hasCollaborator(Long collaboratorId) {
        return (root, query, cb) -> collaboratorId == null
                ? null
                : cb.equal(root.get("collaborator").get("id"), collaboratorId);
    }
}
