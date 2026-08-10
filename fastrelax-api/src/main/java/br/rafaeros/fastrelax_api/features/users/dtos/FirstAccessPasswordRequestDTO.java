package br.rafaeros.fastrelax_api.features.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Definição da senha no primeiro acesso.
 *
 * <p>
 * Não pede a senha atual porque o usuário acabou de usá-la para entrar — pedir de
 * novo seria redundante. Ainda assim a nova precisa ser diferente da temporária,
 * senão a troca não teria efeito.
 */
public record FirstAccessPasswordRequestDTO(
    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 8, max = 100, message = "A nova senha deve ter entre 8 e 100 caracteres")
    String newPassword,

    @NotBlank(message = "A confirmação da nova senha é obrigatória")
    String confirmNewPassword
) {}
