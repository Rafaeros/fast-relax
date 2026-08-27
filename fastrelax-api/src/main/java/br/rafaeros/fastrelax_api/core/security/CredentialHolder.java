package br.rafaeros.fastrelax_api.core.security;

import br.rafaeros.fastrelax_api.features.auth.RefreshToken;

/**
 * Quem tem senha própria no sistema.
 *
 * <p>
 * Desde que o colaborador passou a autenticar com senha, usuário do painel e
 * colaborador têm exatamente o mesmo ciclo de credencial: nascem sem senha
 * utilizável, recebem convite ou senha temporária, são obrigados a definir a
 * própria no primeiro acesso, podem recuperá-la por e-mail e têm as sessões
 * derrubadas a cada troca.
 *
 * <p>
 * Este contrato é o que permite ao {@link CredentialService}, ao
 * {@link CredentialProvisioningService} e ao {@code PasswordRecoveryService}
 * implementarem esse ciclo uma vez só. Sem ele, a regra existiria em duas
 * cópias e a segunda envelheceria — que é como se esquece de revogar refresh
 * token num dos lados.
 */
public interface CredentialHolder {

    Long getId();

    /** Nome de quem recebe o e-mail; usado no cabeçalho e na saudação. */
    String getName();

    /**
     * Endereço para convite e recuperação.
     *
     * <p>
     * Nulo é um estado legítimo: o colaborador de chão de fábrica pode não ter
     * e-mail corporativo, e é justamente esse caso que faz o cadastro cair na
     * senha temporária entregue pelo RH.
     */
    String getEmail();

    String getPasswordHash();

    void setPasswordHash(String passwordHash);

    boolean isMustChangePassword();

    void setMustChangePassword(boolean mustChangePassword);

    /** Sob qual sujeito os tokens desta credencial são emitidos. */
    RefreshToken.SubjectType subjectType();

    /** Atalho de leitura para quem decide entre convite e senha temporária. */
    default boolean hasEmail() {
        String email = getEmail();
        return email != null && !email.isBlank();
    }
}
