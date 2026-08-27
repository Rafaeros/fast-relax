package br.rafaeros.fastrelax_api.features.locations.dtos;

import br.rafaeros.fastrelax_api.features.locations.City;

public record CityResponseDTO(
    Long id,
    Long stateId,
    String stateAcronym,
    String name,
    String ibgeCode
) {
    public CityResponseDTO(City entity) {
        this(entity.getId(),
                entity.getState() != null ? entity.getState().getId() : null,
                entity.getState() != null ? entity.getState().getAcronym() : null,
                entity.getName(), entity.getIbgeCode());
    }
}
