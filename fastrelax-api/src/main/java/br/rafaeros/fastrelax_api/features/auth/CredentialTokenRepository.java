package br.rafaeros.fastrelax_api.features.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CredentialTokenRepository extends JpaRepository<CredentialToken, Long> {

    Optional<CredentialToken> findByTokenHash(String tokenHash);

    /** Pendentes de uma pessoa: emitir um token novo invalida os anteriores. */
    List<CredentialToken> findBySubjectTypeAndSubjectIdAndUsedAtIsNull(
            RefreshToken.SubjectType subjectType, Long subjectId);

    /** Limpeza do que já não serve para nada. */
    @Modifying
    @Query("DELETE FROM CredentialToken t WHERE t.expiresAt < :cutoff OR t.usedAt IS NOT NULL")
    int deleteExpiredAndUsed(@Param("cutoff") LocalDateTime cutoff);
}
