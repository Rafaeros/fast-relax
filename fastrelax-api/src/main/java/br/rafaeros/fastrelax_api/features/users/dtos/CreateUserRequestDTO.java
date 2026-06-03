package br.rafaeros.fastrelax_api.features.users.dtos;

import br.rafaeros.fastrelax_api.features.users.UserRole;

public record CreateUserRequestDTO(
    String name,
    String email,
    String password,
    UserRole role
){}
