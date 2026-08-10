package br.rafaeros.fastrelax_api.features.collaborators.dtos;

public record CollaboratorFilterDTO(
    Long departmentId,
    String name,
    String cpf,
    String phoneNumber,
    Boolean active
) {}
