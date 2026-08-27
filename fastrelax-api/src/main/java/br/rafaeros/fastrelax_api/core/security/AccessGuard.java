package br.rafaeros.fastrelax_api.core.security;

import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.users.UserRole;

/**
 * As perguntas de autorização que os controllers fazem, em um vocabulário só.
 *
 * <p>
 * Registrado como {@code access} para ser chamado dos {@code @PreAuthorize}:
 * {@code @PreAuthorize("@access.operatesCompany()")}. Concentrar as respostas
 * aqui é o que evita a alternativa — cada rota escrevendo sua própria expressão
 * SpEL com a lista de papéis, e uma delas ficando para trás quando um papel novo
 * aparece.
 *
 * <p>
 * Isolamento entre empresas não é assunto deste guarda: disso cuidam o
 * {@code TenantContext} e o {@code CompanyScopedRepository}, que filtram toda
 * consulta. Aqui a pergunta é sempre "este papel pode fazer isto?".
 */
@Component("access")
public class AccessGuard {

    /** Equipe da Physical: administra empresas, firmwares e gestores de cliente. */
    public boolean isPlatformTeam() {
        return hasRole(UserRole.SYSADMIN);
    }

    /** Gestor do cliente: além de operar, cadastra os usuários do painel da empresa. */
    public boolean administersCompany() {
        return hasRole(UserRole.COMPANY_ADMIN);
    }

    /** Gestor ou RH: o dia a dia da empresa — colaboradores, cadeiras, agendamentos. */
    public boolean operatesCompany() {
        return hasRole(UserRole.COMPANY_ADMIN) || hasRole(UserRole.COMPANY_RH);
    }

    public boolean isCollaborator() {
        return Principals.collaborator().isPresent();
    }

    /**
     * Quem pode ver o registro de um colaborador: o próprio, ou quem opera a
     * empresa.
     *
     * <p>
     * Não confere a empresa do alvo — quando o id vem de fora, é o repositório
     * escopado que já o teria descartado, e repetir a checagem aqui daria a falsa
     * impressão de que ela é opcional lá.
     */
    public boolean canAccessCollaborator(Long targetCollaboratorId) {
        if (operatesCompany()) {
            return true;
        }
        return Principals.collaborator()
                .map(logged -> logged.getId().equals(targetCollaboratorId))
                .orElse(false);
    }

    private boolean hasRole(UserRole role) {
        return Principals.user().map(user -> user.getRole() == role).orElse(false);
    }
}
