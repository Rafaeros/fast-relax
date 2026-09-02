package br.rafaeros.fastrelax_api.features.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Recuperação de senha do colaborador.
 *
 * <p>
 * O slug da empresa entra pelo mesmo motivo do login: o e-mail só é único
 * dentro da empresa, e a mesma pessoa pode ser colaboradora de dois clientes.
 * É o mesmo slug que ela já digita para entrar, então não há informação nova
 * a pedir.
 */
public record CollaboratorForgotPasswordRequestDTO(
    @NotBlank(message = "O identificador da empresa é obrigatório")
    String companySlug,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve ser válido")
    String email
) {}
