package br.rafaeros.fastrelax_api.features.users.dtos;

import br.rafaeros.fastrelax_api.features.users.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de usuário do painel.
 *
 * <p>
 * Sem campo de senha de propósito: quem cadastra não escolhe a senha de outra
 * pessoa. O sistema gera uma temporária e a devolve uma única vez na resposta,
 * para ser repassada ao usuário — que é obrigado a trocá-la no primeiro acesso.
 *
 * @param companyId a que empresa o usuário pertence. Só o SYSADMIN informa, ao
 *                  cadastrar o gestor de um cliente; para quem já opera dentro
 *                  de uma empresa o campo é ignorado, e a empresa vem do
 *                  contexto — aceitar o valor do corpo permitiria criar usuário
 *                  na empresa de outro cliente
 */
public record CreateUserRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    String name,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve ser válido")
    @Size(max = 180, message = "O email deve ter no máximo 180 caracteres")
    String email,

    @NotNull(message = "O perfil é obrigatório")
    UserRole role,

    Long companyId
){}
