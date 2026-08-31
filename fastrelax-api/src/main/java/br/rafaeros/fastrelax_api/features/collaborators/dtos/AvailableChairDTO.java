package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.features.chairs.Chair;

/**
 * Cadeira disponível em um horário.
 *
 * <p>
 * Omitimos dados sensíveis como MAC e IP para não expor a infraestrutura
 * para o app do colaborador.
 */
public record AvailableChairDTO(
    Long id,
    String name
) {
    public AvailableChairDTO(Chair chair) {
        this(chair.getId(), chair.getName());
    }
}
