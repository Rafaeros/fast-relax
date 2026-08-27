package br.rafaeros.fastrelax_api.features.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Recuperação de senha do painel: o e-mail é único no sistema inteiro. */
public record ForgotPasswordRequestDTO(
    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve ser válido")
    String email
) {}
