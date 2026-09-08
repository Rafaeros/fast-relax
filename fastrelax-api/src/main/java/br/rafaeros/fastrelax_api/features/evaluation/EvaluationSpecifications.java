package br.rafaeros.fastrelax_api.features.evaluation;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros compostos da listagem de avaliações. Cada fábrica devolve
 * {@code null} quando o argumento está ausente, então o filtro não informado
 * simplesmente some da composição.
 */
public final class EvaluationSpecifications {

    private EvaluationSpecifications() {
    }

    public static Specification<Evaluation> hasScore(Integer score) {
        return (root, query, cb) -> score == null ? null : cb.equal(root.get("score"), score);
    }

    public static Specification<Evaluation> hasCollaborator(Long collaboratorId) {
        return (root, query, cb) -> collaboratorId == null
                ? null
                : cb.equal(root.get("collaborator").get("id"), collaboratorId);
    }

    public static Specification<Evaluation> hasDepartment(Long departmentId) {
        return (root, query, cb) -> departmentId == null
                ? null
                : cb.equal(root.get("collaborator").get("department").get("id"), departmentId);
    }

    /**
     * Intervalo fechado pela data da avaliação. Cada extremo é opcional: informar
     * só um vale como "a partir de" ou "até".
     *
     * <p>
     * O campo é {@code timestamp}, então o fim do intervalo vai até o último
     * instante do dia — comparar com a data crua deixaria de fora tudo que foi
     * avaliado depois da meia-noite do próprio dia informado.
     */
    public static Specification<Evaluation> betweenDates(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from == null) {
                return cb.lessThanOrEqualTo(root.get("evaluationDate"), to.atTime(LocalTime.MAX));
            }
            if (to == null) {
                return cb.greaterThanOrEqualTo(root.get("evaluationDate"), from.atStartOfDay());
            }
            return cb.between(root.get("evaluationDate"), from.atStartOfDay(), to.atTime(LocalTime.MAX));
        };
    }
}
