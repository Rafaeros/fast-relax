package br.rafaeros.fastrelax_api.features.users;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.core.security.CredentialAccount;
import br.rafaeros.fastrelax_api.core.security.CredentialHolder;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import lombok.RequiredArgsConstructor;

/**
 * Acesso às credenciais dos usuários do painel para os fluxos genéricos —
 * convite, recuperação de senha, definição por token.
 *
 * <p>
 * Fica aqui, e não no {@code UserService}, porque a responsabilidade é outra: o
 * serviço aplica regras de cadastro (alcance por empresa, papel que pode ser
 * atribuído), enquanto isto é só a ponte entre um id de token e a entidade.
 * Misturá-los faria a recuperação de senha passar por checagens de tenant que
 * não valem para quem ainda não está logado.
 */
@Component
@RequiredArgsConstructor
public class UserCredentialAccount implements CredentialAccount {

    private final UserRepository userRepository;

    @Override
    public RefreshToken.SubjectType subjectType() {
        return RefreshToken.SubjectType.USER;
    }

    @Override
    public Optional<CredentialHolder> findById(Long id) {
        return userRepository.findById(id).map(CredentialHolder.class::cast);
    }

    /**
     * O {@code companyId} é ignorado: o e-mail do painel é único no sistema
     * inteiro — é ele que identifica o login, e dois clientes com o mesmo
     * endereço tornariam a autenticação ambígua.
     */
    @Override
    public Optional<CredentialHolder> findByEmail(String email, Long companyId) {
        return userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .map(CredentialHolder.class::cast);
    }

    @Override
    public void save(CredentialHolder holder) {
        userRepository.save((User) holder);
    }
}
