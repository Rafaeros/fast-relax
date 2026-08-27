package br.rafaeros.fastrelax_api.features.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Definição de senha a partir do link recebido por e-mail.
 *
 * <p>
 * Serve para convite e para recuperação: os dois chegam ao mesmo ponto — a
 * pessoa provou controlar a caixa de entrada e agora escolhe a senha. Sem campo
 * de senha atual, que em um dos casos não existe e no outro foi esquecida.
 */
public record ResetPasswordRequestDTO(
    @NotBlank(message = "O link de redefinição é obrigatório")
    String token,

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 8, max = 100, message = "A nova senha deve ter entre 8 e 100 caracteres")
    String newPassword,

    @NotBlank(message = "A confirmação da nova senha é obrigatória")
    String confirmNewPassword
) {}
