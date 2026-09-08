package br.rafaeros.fastrelax_api.features.evaluation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.evaluation.Evaluation;
import br.rafaeros.fastrelax_api.features.evaluation.EvaluationScore;

/**
 * Avaliação como a tela do RH precisa ler: com o nome de quem avaliou e a
 * massagem correspondente, não só os ids.
 *
 * @param scoreLabel mesma nota em português, pronta para exibição
 */
public record EvaluationResponseDTO(
    Long id,
    Long collaboratorId,
    String collaboratorName,
    String departmentName,
    Long sessionId,
    LocalDate sessionDate,
    String chairName,
    Integer score,
    String scoreLabel,
    String comments,
    LocalDateTime evaluationDate
) {
    public EvaluationResponseDTO(Evaluation evaluation) {
        this(
            evaluation.getId(),
            evaluation.getCollaborator().getId(),
            evaluation.getCollaborator().getName(),
            evaluation.getCollaborator().getDepartment() != null
                ? evaluation.getCollaborator().getDepartment().getName()
                : null,
            evaluation.getSession().getId(),
            evaluation.getSession().getSessionDate(),
            evaluation.getSession().getChair() != null
                ? evaluation.getSession().getChair().getName()
                : null,
            evaluation.getScore(),
            EvaluationScore.labelOf(evaluation.getScore()),
            evaluation.getComments(),
            evaluation.getEvaluationDate()
        );
    }
}
