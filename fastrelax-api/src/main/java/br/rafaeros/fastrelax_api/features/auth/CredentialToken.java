package br.rafaeros.fastrelax_api.features.auth;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de uso único enviado por e-mail para definir senha.
 *
 * <p>
 * Mesma linha do {@link RefreshToken}: o valor sorteado vai para o destinatário
 * e o banco guarda só o SHA-256. Um vazamento desta tabela não permite definir a
 * senha de ninguém.
 *
 * <p>
 * A validade é curta porque o link circula por um canal que não controlamos —
 * caixa de entrada encaminhada, histórico de navegador, print em grupo.
 */
@Entity
@Table(name = "credential_tokens")
@NoArgsConstructor
@Getter
@Setter
public class CredentialToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 do token entregue por e-mail; o valor em claro nunca é persistido. */
    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "TEXT")
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private RefreshToken.SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Por que o token foi emitido.
     *
     * <p>
     * A diferença não é técnica — os dois definem senha do mesmo jeito. Ela
     * existe para o texto do e-mail e para a tela saberem o que dizer: "defina
     * sua senha" e "redefina sua senha" chegam em momentos muito diferentes.
     */
    public enum Purpose {
        /** Conta recém-criada, ainda sem senha utilizável. */
        INVITE,
        /** Recuperação pedida por alguém que já tinha senha. */
        RESET
    }
}
