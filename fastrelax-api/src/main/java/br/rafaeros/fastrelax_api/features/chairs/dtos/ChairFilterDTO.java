package br.rafaeros.fastrelax_api.features.chairs.dtos;

/**
 * @param name   busca parcial por nome
 * @param active situação cadastral
 * @param online presença derivada do último heartbeat, não uma coluna
 */
public record ChairFilterDTO(
    String name,
    Boolean active,
    Boolean online
) {}
