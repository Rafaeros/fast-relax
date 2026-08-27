package br.rafaeros.fastrelax_api.features.collaborators.dtos;

import br.rafaeros.fastrelax_api.core.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCollaboratorDTO(
    @NotNull(message = "O departamento é obrigatório")
    Long departmentId,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    String name,

    /**
     * Opcional: omitido ou vazio mantém o CPF atual. Só informe para corrigir um
     * cadastro errado — trocar o CPF troca a credencial de login do colaborador.
     */
    @Pattern(regexp = "^(|\\d{11})$", message = "O CPF deve conter exatamente 11 dígitos, sem pontuação")
    @Cpf
    String cpf,

    @NotBlank(message = "O telefone é obrigatório")
    @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
    String phoneNumber,

    /**
     * Em branco remove o e-mail do cadastro — e, com ele, a possibilidade de a
     * pessoa recuperar a senha sozinha. Não afeta a senha atual.
     */
    @Email(message = "O email deve ser válido")
    @Size(max = 180, message = "O email deve ter no máximo 180 caracteres")
    String email,

    boolean active
) {}
