package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.core.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCollaboratorRequestDTO(
    @NotNull(message = "O departamento é obrigatório")
    Long departmentId,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    String name,

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos, sem pontuação")
    @Cpf
    String cpf,

    /** Opcional, como o e-mail: parte do quadro não tem telefone cadastrado. */
    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    String phoneNumber,

    /**
     * Opcional. Preenchido, a pessoa recebe um convite e define a própria senha;
     * em branco, o sistema gera uma temporária para o RH repassar.
     */
    @Email(message = "O email deve ser válido")
    @Size(max = 180, message = "O email deve ter no máximo 180 caracteres")
    String email
) {}
