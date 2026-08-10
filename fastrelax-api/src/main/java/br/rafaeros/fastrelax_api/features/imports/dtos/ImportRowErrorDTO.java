package br.rafaeros.fastrelax_api.features.imports.dtos;

/**
 * Falha de uma linha específica.
 *
 * @param row    número da linha na planilha, como o usuário vê no Excel
 * @param name   nome lido, para localizar a linha sem abrir o arquivo
 * @param reason motivo em linguagem de negócio
 */
public record ImportRowErrorDTO(
    int row,
    String name,
    String reason
) {}
