package br.rafaeros.fastrelax_api.features.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais do colaborador.
 *
 * <p>
 * O slug da empresa entra porque o CPF só é único dentro da empresa: sem ele a
 * busca seria ambígua no momento em que a mesma pessoa fosse colaboradora de
 * dois clientes.
 *
 * <p>
 * Nenhum dos três campos tem validação de formato além de estar preenchido —
 * nem {@code @Cpf}, nem o formato do slug. Numa tela de login, um erro de
 * validação distinguindo "slug inválido" de "credenciais inválidas" é
 * justamente o que permite mapear quem existe: toda tentativa recusada tem que
 * responder a mesma coisa.
 */
public record CollaboratorLoginRequestDTO(
    @NotBlank(message = "O identificador da empresa é obrigatório")
    String companySlug,

    @NotBlank(message = "O CPF é obrigatório")
    String cpf,

    @NotBlank(message = "A senha é obrigatória")
    String password
) {}
