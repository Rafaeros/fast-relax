package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.Optional;

import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.core.security.CredentialAccount;
import br.rafaeros.fastrelax_api.core.security.CredentialHolder;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import lombok.RequiredArgsConstructor;

/**
 * Acesso às credenciais dos colaboradores para os fluxos genéricos — convite,
 * recuperação de senha, definição por token.
 *
 * <p>
 * Usa o repositório direto, sem os métodos escopados: estes fluxos rodam para
 * quem <em>não está logado</em>, então não há tenant no contexto. Quem garante o
 * isolamento aqui é o próprio token, que aponta para um id específico, e o CNPJ
 * informado na recuperação.
 */
@Component
@RequiredArgsConstructor
public class CollaboratorCredentialAccount implements CredentialAccount {

    private final CollaboratorRepository collaboratorRepository;

    @Override
    public RefreshToken.SubjectType subjectType() {
        return RefreshToken.SubjectType.COLLABORATOR;
    }

    @Override
    public Optional<CredentialHolder> findById(Long id) {
        return collaboratorRepository.findById(id).map(CredentialHolder.class::cast);
    }

    /**
     * O e-mail não é mais único dentro da empresa — só o CPF é. Duas pessoas
     * podem compartilhar endereço; a recuperação cai sempre no cadastro mais
     * recente entre os ativos, e quem não é o dono pede de novo depois de o RH
     * corrigir o cadastro duplicado.
     *
     * <p>
     * Sem o {@code companyId} a busca seria ambígua entre clientes, então ele é
     * obrigatório aqui.
     */
    @Override
    public Optional<CredentialHolder> findByEmail(String email, Long companyId) {
        if (companyId == null) {
            return Optional.empty();
        }
        return collaboratorRepository.findByCompanyIdAndEmailIgnoreCaseOrderByCreatedAtDesc(companyId, email)
                .stream()
                .filter(Collaborator::isEnabled)
                .findFirst()
                .map(CredentialHolder.class::cast);
    }

    @Override
    public void save(CredentialHolder holder) {
        collaboratorRepository.save((Collaborator) holder);
    }
}
