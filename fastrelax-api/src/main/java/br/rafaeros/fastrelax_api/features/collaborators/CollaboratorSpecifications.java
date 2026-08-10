package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable filters for {@link Collaborator} queries.
 * Each factory returns {@code null} when its argument is absent, so callers can
 * combine them with {@link Specification#allOf} and unset filters are ignored.
 */
public final class CollaboratorSpecifications {

    private CollaboratorSpecifications() {
    }

    public static Specification<Collaborator> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Matches on the blind index, so the caller must pass the HMAC of a complete
     * CPF. Partial matching is impossible by design: the stored ciphertext has no
     * relation to the digits of the plaintext.
     */
    public static Specification<Collaborator> cpfHashEquals(String cpfHash) {
        return (root, query, cb) -> (cpfHash == null || cpfHash.isBlank())
                ? null
                : cb.equal(root.get("cpfHash"), cpfHash);
    }

    public static Specification<Collaborator> phoneNumberContains(String phoneNumber) {
        return (root, query, cb) -> (phoneNumber == null || phoneNumber.isBlank())
                ? null
                : cb.like(cb.lower(root.get("phoneNumber")), "%" + phoneNumber.toLowerCase() + "%");
    }

    public static Specification<Collaborator> hasActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? null
                : cb.equal(root.get("active"), active);
    }

    public static Specification<Collaborator> inDepartment(Long departmentId) {
        return (root, query, cb) -> departmentId == null
                ? null
                : cb.equal(root.get("department").get("id"), departmentId);
    }
}
