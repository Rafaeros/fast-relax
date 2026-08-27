package br.rafaeros.fastrelax_api.core.security;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.users.User;

/**
 * Quem está autenticado agora.
 *
 * <p>
 * Ler o principal exige três passos que é fácil escrever pela metade — checar se
 * há autenticação, checar se ela é de verdade, conferir o tipo. Metade dos
 * serviços fazia isso à mão, cada um com sua mensagem de erro; aqui é um lugar
 * só, com o mesmo comportamento em todos.
 */
public final class Principals {

    private Principals() {
    }

    public static Optional<Collaborator> collaborator() {
        return of(Collaborator.class);
    }

    public static Optional<User> user() {
        return of(User.class);
    }

    public static Collaborator requireCollaborator() {
        return collaborator().orElseThrow(() -> new AccessDeniedException(
                "Rota disponível apenas para colaboradores autenticados"));
    }

    public static User requireUser() {
        return user().orElseThrow(() -> new AccessDeniedException(
                "Rota disponível apenas para usuários do painel autenticados"));
    }

    public static <T> Optional<T> of(Class<T> type) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return type.isInstance(principal) ? Optional.of(type.cast(principal)) : Optional.empty();
    }
}
