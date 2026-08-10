package br.rafaeros.fastrelax_api.features.users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Troca da própria senha: exige a atual para impedir uso de sessão esquecida. */
public record ChangePasswordRequestDTO(
    @NotBlank(message = "A senha atual é obrigatória")
    String currentPassword,

    @NotBlank(message = "A nova senha é obrigatória")
    @Size(min = 8, max = 100, message = "A nova senha deve ter entre 8 e 100 caracteres")
    String newPassword,

    /** Precisa ser idêntica a {@code newPassword}: protege contra erro de digitação. */
    @NotBlank(message = "A confirmação da nova senha é obrigatória")
    String confirmNewPassword
) {}
