package br.rafaeros.fastrelax_api.features.auth;

public record LoginRequestDTO (
    String email,
    String password
) {
    
}
