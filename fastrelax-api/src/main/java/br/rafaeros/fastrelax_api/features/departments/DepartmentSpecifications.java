package br.rafaeros.fastrelax_api.features.departments;

import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable, composable filters for {@link Department} queries.
 * Each factory returns {@code null} when its argument is absent, so callers can
 * combine them with {@link Specification#allOf} and unset filters are ignored.
 */
public final class DepartmentSpecifications {

    private DepartmentSpecifications() {
    }

    public static Specification<Department> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Department> hasActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? null
                : cb.equal(root.get("active"), active);
    }
}
