package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.users.User;
import br.rafaeros.fastrelax_api.features.users.UserRole;

@Component("collaboratorSecurity")
public class CollaboratorSecurity {

    public boolean canAccessCollaborator(Long targetCollaboratorId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        var principal = authentication.getPrincipal();

        // Se for um User (ADMIN/RH), tem acesso a tudo
        if (principal instanceof User user) {
            return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RH;
        }

        // Se for um Collaborator, só pode acessar seus próprios dados
        if (principal instanceof Collaborator loggedCollab) {
            return loggedCollab.getId().equals(targetCollaboratorId);
        }

        return false;
    }

    public boolean hasAdminOrRhAccess() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        var principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.RH;
        }

        return false;
    }
}
