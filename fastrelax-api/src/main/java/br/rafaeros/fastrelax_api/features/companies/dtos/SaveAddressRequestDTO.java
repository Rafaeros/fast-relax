package br.rafaeros.fastrelax_api.features.companies.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveAddressRequestDTO(
    @NotNull(message = "A cidade é obrigatória")
    Long cityId,

    @NotBlank(message = "O CEP é obrigatório")
    @Size(max = 10, message = "O CEP deve ter no máximo 10 caracteres")
    String cep,

    @NotBlank(message = "O logradouro é obrigatório")
    String street,

    @NotBlank(message = "O número é obrigatório")
    @Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
    String number,

    String complement
) {}
