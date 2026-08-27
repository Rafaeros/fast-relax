package br.rafaeros.fastrelax_api.core.security;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.auth.RefreshToken;

/**
 * Escolhe o {@link CredentialAccount} certo pelo tipo de sujeito.
 *
 * <p>
 * Recebe as implementações por injeção de lista: registrar um tipo novo de
 * credencial é criar o bean, sem tocar aqui.
 */
@Component
public class CredentialAccounts {

    private final Map<RefreshToken.SubjectType, CredentialAccount> byType =
            new EnumMap<>(RefreshToken.SubjectType.class);

    public CredentialAccounts(List<CredentialAccount> accounts) {
        for (CredentialAccount account : accounts) {
            CredentialAccount previous = byType.put(account.subjectType(), account);
            if (previous != null) {
                // Duas implementações para o mesmo tipo significa que uma delas
                // nunca rodaria, e qual seria dependeria da ordem de varredura.
                throw new IllegalStateException(
                        "Mais de um CredentialAccount para " + account.subjectType());
            }
        }
    }

    public Optional<CredentialAccount> of(RefreshToken.SubjectType subjectType) {
        return Optional.ofNullable(byType.get(subjectType));
    }

    /** A credencial apontada por um token, seja ela de qual tipo for. */
    public Optional<CredentialHolder> find(RefreshToken.SubjectType subjectType, Long id) {
        return of(subjectType).flatMap(account -> account.findById(id));
    }

    public void save(CredentialHolder holder) {
        of(holder.subjectType())
                .orElseThrow(() -> new IllegalStateException(
                        "Sem CredentialAccount para " + holder.subjectType()))
                .save(holder);
    }
}
