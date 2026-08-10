package br.rafaeros.fastrelax_api.features.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findBySubjectTypeAndSubjectIdAndRevokedAtIsNull(RefreshToken.SubjectType subjectType,
            Long subjectId);

    /** Limpeza dos tokens que já não servem para nada. */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff OR t.revokedAt IS NOT NULL")
    int deleteExpiredAndRevoked(@Param("cutoff") LocalDateTime cutoff);
}
