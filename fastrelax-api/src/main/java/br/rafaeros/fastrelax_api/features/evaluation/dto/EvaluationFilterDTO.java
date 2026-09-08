package br.rafaeros.fastrelax_api.features.evaluation.dto;

import java.time.LocalDate;

/**
 * @param score          nota exata, para o RH abrir só as notas baixas
 * @param collaboratorId ignorado quando quem consulta é colaborador — ele só vê
 *                       as próprias avaliações
 * @param from           início do intervalo pela data da avaliação, inclusivo
 * @param to             fim do intervalo, inclusivo
 */
public record EvaluationFilterDTO(
    Integer score,
    Long collaboratorId,
    Long departmentId,
    LocalDate from,
    LocalDate to
) {}
