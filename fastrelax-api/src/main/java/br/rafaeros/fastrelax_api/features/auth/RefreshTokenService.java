package br.rafaeros.fastrelax_api.features.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;

/**
 * Emissão e rotação de refresh tokens.
 *
 * <p>
 * O token é opaco e aleatório, não um JWT: como o servidor precisa poder
 * invalidá-lo antes do vencimento, ele tem que existir no banco de qualquer
 * forma — e aí um JWT só acrescentaria tamanho.
 *
 * <p>
 * Cada uso <b>rotaciona</b>: o token apresentado é revogado e um novo é emitido.
 * Se um token já usado reaparecer, é sinal de vazamento, e todas as sessões
 * daquele usuário são derrubadas.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${api.security.refresh-token.expiration-days:30}")
    private int expirationDays;

    /** @return o token em claro — é a única vez que ele existe fora do cliente */
    @Transactional
    public String issue(RefreshToken.SubjectType subjectType, Long subjectId) {
        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hash(token));
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        refreshTokenRepository.save(entity);

        return token;
    }

    /**
     * Valida e consome o token, devolvendo o dono. O chamador deve emitir um novo
     * par de tokens em seguida.
     */
    @Transactional
    public RefreshToken consume(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Refresh token é obrigatório");
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new BusinessException("Refresh token inválido"));

        if (stored.getRevokedAt() != null) {
            // Token já usado reaparecendo: trata como comprometido e derruba tudo.
            revokeAllFor(stored.getSubjectType(), stored.getSubjectId());
            throw new BusinessException("Refresh token já utilizado. Faça login novamente.");
        }
        if (!stored.isUsable()) {
            throw new BusinessException("Refresh token expirado. Faça login novamente.");
        }

        stored.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);
        return stored;
    }

    /** Logout: invalida o token apresentado, sem derrubar os outros dispositivos. */
    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(token)).ifPresent(stored -> {
            if (stored.getRevokedAt() == null) {
                stored.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(stored);
            }
        });
    }

    /** Derruba todas as sessões do usuário — desativação, troca de CPF, suspeita de vazamento. */
    @Transactional
    public void revokeAllFor(RefreshToken.SubjectType subjectType, Long subjectId) {
        List<RefreshToken> active = refreshTokenRepository
                .findBySubjectTypeAndSubjectIdAndRevokedAtIsNull(subjectType, subjectId);
        LocalDateTime now = LocalDateTime.now();
        active.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(active);
    }

    @Transactional
    public int purgeExpired() {
        return refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
