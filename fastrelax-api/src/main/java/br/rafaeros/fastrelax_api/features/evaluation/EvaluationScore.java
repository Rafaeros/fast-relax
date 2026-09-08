package br.rafaeros.fastrelax_api.features.evaluation;

import java.util.Arrays;

/**
 * A escala de 1 a 5 e o que cada degrau significa.
 *
 * <p>
 * O valor trafegado continua sendo o número — é o que a CHECK constraint aceita
 * e o que o app envia ao tocar num dos cinco rostos. O rótulo vai num campo
 * separado do DTO para a tela do RH não manter um dicionário próprio, na mesma
 * linha do {@code statusLabel} das sessões.
 */
public enum EvaluationScore {

    TERRIBLE(1, "Péssimo"),
    BAD(2, "Ruim"),
    REGULAR(3, "Regular"),
    GOOD(4, "Bom"),
    EXCELLENT(5, "Excelente");

    private final int value;
    private final String label;

    EvaluationScore(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    /** Rótulo de uma nota já validada; nota fora da escala vira {@code null}. */
    public static String labelOf(Integer score) {
        if (score == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.value == score)
                .map(EvaluationScore::getLabel)
                .findFirst()
                .orElse(null);
    }
}
