package br.rafaeros.fastrelax_api.features.locations.dtos;

import br.rafaeros.fastrelax_api.features.locations.State;

public record StateResponseDTO(
    Long id,
    String name,
    String acronym,
    String ibgeCode
) {
    public StateResponseDTO(State entity) {
        this(entity.getId(), entity.getName(), entity.getAcronym(), entity.getIbgeCode());
    }
}
