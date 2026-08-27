package br.rafaeros.fastrelax_api.core.security;

import java.util.Optional;

import br.rafaeros.fastrelax_api.features.auth.RefreshToken;

/**
 * Como alcançar as credenciais de um tipo de conta.
 *
 * <p>
 * O token de convite e o de recuperação guardam um par
 * {@code (subject_type, subject_id)} — precisam voltar dali para a entidade sem
 * saber se é usuário do painel ou colaborador. Uma implementação por tipo, e o
 * {@link CredentialAccounts} escolhe.
 *
 * <p>
 * A alternativa seria um {@code switch} sobre o tipo repetido em cada fluxo
 * (convite, recuperação, definição de senha). Bastaria um terceiro tipo de
 * credencial aparecer para um deles ficar para trás — em silêncio, porque
 * {@code default} não é erro de compilação.
 */
public interface CredentialAccount {

    RefreshToken.SubjectType subjectType();

    Optional<CredentialHolder> findById(Long id);

    /**
     * Conta vigente com aquele e-mail, para a recuperação de senha.
     *
     * @param companyId empresa a considerar; ignorado por contas globais como as
     *                  do painel, cujo e-mail é único no sistema inteiro
     */
    Optional<CredentialHolder> findByEmail(String email, Long companyId);

    void save(CredentialHolder holder);
}
