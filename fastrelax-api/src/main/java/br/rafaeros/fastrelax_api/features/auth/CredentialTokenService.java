package br.rafaeros.fastrelax_api.features.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.security.CredentialHolder;
import lombok.RequiredArgsConstructor;

/**
 * Emissão e consumo dos tokens que chegam por e-mail.
 *
 * <p>
 * Mesma construção do {@link RefreshTokenService}, e pelo mesmo motivo: o valor
 * é aleatório e opaco, o banco guarda só o hash, e o uso é único.
 *
 * <p>
 * Emitir invalida os pendentes daquela pessoa. Sem isso, pedir "esqueci minha
 * senha" três vezes deixaria três links vivos — e o mais antigo, que já pode ter
 * sido encaminhado ou vazado, continuaria valendo.
 */
@Service
@RequiredArgsConstructor
public class CredentialTokenService {

    /**
     * 48 bytes. O token viaja em URL e é a única barreira entre um e-mail
     * interceptado e a conta: não há aqui o limite de tentativas que protege uma
     * senha curta.
     */
    private static final int TOKEN_BYTES = 48;

    private final CredentialTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    /** Convite: a pessoa ainda não tem senha, então a janela é folgada. */
    @Value("${app.security.credential-token.invite-hours:48}")
    private long inviteHours;

    /**
     * Recuperação: janela curta de propósito. Quem pediu está com a caixa de
     * entrada aberta agora, e a senha atual continua funcionando enquanto isso.
     */
    @Value("${app.security.credential-token.reset-hours:2}")
    private long resetHours;

    /**
     * @return o token em claro — a única vez que ele existe fora do e-mail
     */
    @Transactional
    public String issue(CredentialHolder holder, CredentialToken.Purpose purpose) {
        invalidatePending(holder);

        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        CredentialToken entity = new CredentialToken();
        entity.setTokenHash(hash(token));
        entity.setPurpose(purpose);
        entity.setSubjectType(holder.subjectType());
        entity.setSubjectId(holder.getId());
        entity.setExpiresAt(LocalDateTime.now().plusHours(validityHours(purpose)));
        tokenRepository.save(entity);

        return token;
    }

    /** Quantas horas o link daquele tipo vale — o e-mail informa isso a quem recebe. */
    public long validityHours(CredentialToken.Purpose purpose) {
        return purpose == CredentialToken.Purpose.INVITE ? inviteHours : resetHours;
    }

    /**
     * Token válido, sem consumir.
     *
     * <p>
     * É o que a tela usa ao abrir o link: saber de quem é e por que foi emitido
     * permite saudar a pessoa e escolher o texto certo. Consumir aqui gastaria o
     * token só por carregar a página — e um preview de link em aplicativo de
     * mensagem já invalidaria o convite antes de alguém clicar.
     */
    @Transactional(readOnly = true)
    public Optional<CredentialToken> peek(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByTokenHash(hash(token)).filter(CredentialToken::isUsable);
    }

    /**
     * Valida e marca como usado.
     *
     * @return vazio quando o token não existe, já foi usado ou venceu
     */
    @Transactional
    public Optional<CredentialToken> consume(String token) {
        Optional<CredentialToken> found = peek(token);

        found.ifPresent(stored -> {
            stored.setUsedAt(LocalDateTime.now());
            tokenRepository.save(stored);
        });

        return found;
    }

    /**
     * Derruba os links pendentes de uma pessoa.
     *
     * <p>
     * Chamado ao emitir um token novo e depois de qualquer troca de senha: se a
     * troca foi motivada por suspeita de acesso indevido, um link de redefinição
     * ainda vivo anularia a medida.
     */
    @Transactional
    public void invalidatePending(CredentialHolder holder) {
        List<CredentialToken> pending = tokenRepository
                .findBySubjectTypeAndSubjectIdAndUsedAtIsNull(holder.subjectType(), holder.getId());

        LocalDateTime now = LocalDateTime.now();
        pending.forEach(token -> token.setUsedAt(now));
        tokenRepository.saveAll(pending);
    }

    @Transactional
    public int purgeExpired() {
        return tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());
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
