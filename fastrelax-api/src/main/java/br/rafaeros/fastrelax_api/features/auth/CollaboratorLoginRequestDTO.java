package br.rafaeros.fastrelax_api.features.auth;

import br.rafaeros.fastrelax_api.core.validation.Cpf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CollaboratorLoginRequestDTO(
    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos, sem pontuação")
    @Cpf
    String cpf
) {}
