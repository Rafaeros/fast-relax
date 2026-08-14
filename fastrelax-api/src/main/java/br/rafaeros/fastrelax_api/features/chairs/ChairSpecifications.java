package br.rafaeros.fastrelax_api.features.chairs;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros combináveis de {@link Chair}. Cada fábrica devolve {@code null} quando
 * o argumento está ausente, então filtros não informados somem da composição.
 */
public final class ChairSpecifications {

    private ChairSpecifications() {
    }

    public static Specification<Chair> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Chair> hasActive(Boolean active) {
        return (root, query, cb) -> active == null
                ? null
                : cb.equal(root.get("active"), active);
    }

    /**
     * Presença é derivada do último heartbeat: online significa ter batido depois
     * do limite e ter IP conhecido. Traduzir isso para SQL evita carregar todas as
     * cadeiras só para filtrar em memória, e mantém a paginação correta.
     */
    public static Specification<Chair> isOnline(Boolean online, int offlineAfterSeconds) {
        return (root, query, cb) -> {
            if (online == null) {
                return null;
            }
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(offlineAfterSeconds);

            if (online) {
                return cb.and(
                        cb.isNotNull(root.get("ipAddress")),
                        cb.isNotNull(root.get("lastSeenAt")),
                        cb.greaterThan(root.get("lastSeenAt"), threshold));
            }
            return cb.or(
                    cb.isNull(root.get("ipAddress")),
                    cb.isNull(root.get("lastSeenAt")),
                    cb.lessThanOrEqualTo(root.get("lastSeenAt"), threshold));
        };
    }
}
