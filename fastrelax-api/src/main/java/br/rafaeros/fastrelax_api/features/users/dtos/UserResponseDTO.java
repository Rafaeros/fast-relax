package br.rafaeros.fastrelax_api.features.users.dtos;

import br.rafaeros.fastrelax_api.features.users.User;
import br.rafaeros.fastrelax_api.features.users.UserRole;

public record UserResponseDTO (
    Long id,
    String name,
    String email,
    UserRole role,
    /** Mesmo perfil em português, pronto para exibição. */
    String roleLabel,
    /** Verdadeiro enquanto o usuário ainda não definiu a própria senha. */
    boolean mustChangePassword,
    boolean active
){
    public UserResponseDTO(User user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getRole() != null ? user.getRole().getLabel() : null,
                user.isMustChangePassword(), user.isActive());
    }
}
