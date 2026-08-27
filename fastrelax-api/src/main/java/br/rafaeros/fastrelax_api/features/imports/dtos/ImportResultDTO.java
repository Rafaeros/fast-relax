package br.rafaeros.fastrelax_api.features.imports.dtos;

import java.util.List;

/**
 * Resumo da importação. Linhas com erro não interrompem o arquivo: são puladas e
 * relatadas aqui, para o RH corrigir só o que falhou e reenviar.
 *
 * @param credentials senhas temporárias dos colaboradores criados nesta
 *                    importação, exibidas uma única vez
 */
public record ImportResultDTO(
    int totalRows,
    int processed,
    int failed,
    int departmentsCreated,
    int collaboratorsCreated,
    int collaboratorsUpdated,
    int schedulesSaved,
    List<ImportRowErrorDTO> errors,
    List<ImportedCredentialDTO> credentials
) {}
