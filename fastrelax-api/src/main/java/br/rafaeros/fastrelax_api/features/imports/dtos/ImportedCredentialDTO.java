package br.rafaeros.fastrelax_api.features.imports.dtos;

import br.rafaeros.fastrelax_api.core.dto.CredentialDeliveryDTO;

/**
 * Como o acesso de um colaborador criado pela importação foi entregue.
 *
 * <p>
 * Só os criados entram na lista. Reimportar a mesma planilha atualiza cadastro,
 * e trocar a credencial de quem já usa o app quebraria o acesso de todo mundo a
 * cada correção de telefone.
 *
 * @param cpf mascarado ({@code ***.456.789-**}): a lista costuma ser exibida em
 *            tela e impressa, e o CPF inteiro não precisa estar ali para o RH
 *            saber de quem é a linha
 */
public record ImportedCredentialDTO(
    String name,
    String cpf,
    CredentialDeliveryDTO delivery
) {}
