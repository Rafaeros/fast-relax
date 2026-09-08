package br.rafaeros.fastrelax_api.features.evaluation.dto;

import java.util.List;

/**
 * Os números do topo da tela do RH.
 *
 * <p>
 * A distribuição vem sempre com as cinco notas, inclusive as que ninguém deu:
 * um gráfico com barras faltando não mostra que ninguém avaliou com 1, mostra
 * que a nota 1 não existe.
 *
 * @param total       avaliações no período filtrado
 * @param average     média das notas; {@code null} quando não há nenhuma
 * @param sessionsDone massagens concluídas no mesmo período — a base da adesão
 * @param responseRate percentual de sessões concluídas que receberam nota
 */
public record EvaluationSummaryDTO(
    long total,
    Double average,
    long sessionsDone,
    Double responseRate,
    List<ScoreCountDTO> distribution
) {
    /**
     * @param score nota de 1 a 5
     * @param label mesma nota em português
     * @param total quantas avaliações com essa nota
     */
    public record ScoreCountDTO(int score, String label, long total) {
    }
}
