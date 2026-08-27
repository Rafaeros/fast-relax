package br.rafaeros.fastrelax_api.core.security;

import java.util.Base64;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.util.TemporaryPasswordGenerator;
import br.rafaeros.fastrelax_api.features.auth.CredentialTokenService;
import br.rafaeros.fastrelax_api.features.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;

/**
 * O ciclo de vida de uma senha, para qualquer credencial do sistema.
 *
 * <p>
 * Usuário do painel e colaborador seguem exatamente as mesmas regras — a conta
 * nasce sem senha utilizável, recebe convite ou temporária, troca obrigatória no
 * primeiro acesso, confirmação conferida, nova senha diferente da atual, e tudo
 * que dava acesso cai a cada troca. Manter isso em um lugar só é o que impede um
 * dos dois lados de esquecer a revogação e deixar um refresh token válido
 * sobreviver à troca de senha.
 *
 * <p>
 * Nenhum método aqui persiste a entidade: cada dono tem seu repositório, e
 * quem chama grava. O que este serviço garante é o estado correto do objeto e a
 * revogação das sessões.
 */
@Service
@RequiredArgsConstructor
public class CredentialService {

    /**
     * Hash descartável de uma senha que não existe, usado para gastar o mesmo
     * tempo de bcrypt quando a credencial não foi encontrada.
     */
    private static final String ABSENT_CREDENTIAL_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private static final java.security.SecureRandom UNUSABLE_RANDOM = new java.security.SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final CredentialTokenService credentialTokenService;

    /** Verificação de login. */
    public boolean matches(CredentialHolder holder, String rawPassword) {
        return passwordEncoder.matches(rawPassword, holder.getPasswordHash());
    }

    /**
     * Gasta o tempo de um bcrypt sem ter contra o que comparar.
     *
     * <p>
     * Sem isto, um login com CPF inexistente responderia visivelmente mais rápido
     * que um com CPF certo e senha errada — e essa diferença é suficiente para
     * enumerar quem é colaborador de uma empresa.
     */
    public void wasteMatch(String rawPassword) {
        passwordEncoder.matches(rawPassword == null ? "" : rawPassword, ABSENT_CREDENTIAL_HASH);
    }

    /**
     * Senha sorteada e descartada na hora.
     *
     * <p>
     * Para a conta que vai receber convite por e-mail: a coluna é
     * {@code NOT NULL} e o {@code UserDetails} do Spring não aceita senha nula,
     * mas ninguém — nem quem cadastrou — pode conhecer um valor que a pessoa
     * ainda não escolheu. O login simplesmente não confere até o convite ser
     * aceito.
     */
    public void initializeWithUnusablePassword(CredentialHolder holder) {
        byte[] raw = new byte[32];
        UNUSABLE_RANDOM.nextBytes(raw);
        holder.setPasswordHash(passwordEncoder.encode(Base64.getEncoder().encodeToString(raw)));
        holder.setMustChangePassword(true);
    }

    /**
     * Prepara uma credencial recém-criada.
     *
     * @return a senha temporária em claro — a única vez que ela existe fora do
     *         cliente, já que o banco guarda apenas o hash
     */
    public String initializeWithTemporaryPassword(CredentialHolder holder) {
        String temporary = TemporaryPasswordGenerator.generate();
        holder.setPasswordHash(passwordEncoder.encode(temporary));
        holder.setMustChangePassword(true);
        return temporary;
    }

    /** Troca da própria senha, conferindo a atual. */
    public void changePassword(CredentialHolder holder, String currentPassword, String newPassword,
            String confirmation) {
        if (!matches(holder, currentPassword)) {
            throw new BusinessException("Senha atual incorreta");
        }
        applyNewPassword(holder, newPassword, confirmation);
    }

    /**
     * Define a senha sem conferir a atual.
     *
     * <p>
     * Para quem chegou por link de convite ou de recuperação: a prova de
     * identidade foi controlar a caixa de entrada, e pedir a senha antiga ali
     * seria absurdo — em um dos casos ela não existe, no outro foi esquecida.
     */
    public void changePasswordWithoutCurrent(CredentialHolder holder, String newPassword,
            String confirmation) {
        applyNewPassword(holder, newPassword, confirmation);
    }

    /** Primeiro acesso: o dono define a própria senha e destrava o resto da API. */
    public void defineFirstAccessPassword(CredentialHolder holder, String newPassword, String confirmation) {
        if (!holder.isMustChangePassword()) {
            throw new BusinessException("Sua senha já foi definida. Use a troca de senha para alterá-la.");
        }
        applyNewPassword(holder, newPassword, confirmation);
        holder.setMustChangePassword(false);
    }

    /**
     * Redefinição por quem administra, pelo mesmo princípio do cadastro: ninguém
     * escolhe a senha de outra pessoa. Gera uma temporária, devolve uma única vez
     * e obriga o dono a trocá-la no próximo acesso.
     */
    public String resetToTemporaryPassword(CredentialHolder holder) {
        String temporary = TemporaryPasswordGenerator.generate();
        holder.setMustChangePassword(true);
        write(holder, temporary);
        return temporary;
    }

    private void applyNewPassword(CredentialHolder holder, String newPassword, String confirmation) {
        if (newPassword == null || !newPassword.equals(confirmation)) {
            throw new BusinessException("A confirmação não confere com a nova senha");
        }
        if (matches(holder, newPassword)) {
            throw new BusinessException("A nova senha deve ser diferente da atual");
        }
        write(holder, newPassword);
    }

    /**
     * Grava o hash e derruba tudo que ainda daria acesso.
     *
     * <p>
     * Sessões abertas e links de convite/recuperação pendentes caem juntos: se a
     * troca foi motivada por suspeita de acesso indevido, deixar qualquer um dos
     * dois de pé anularia a medida.
     */
    private void write(CredentialHolder holder, String rawPassword) {
        holder.setPasswordHash(passwordEncoder.encode(rawPassword));
        refreshTokenService.revokeAllFor(holder.subjectType(), holder.getId());
        credentialTokenService.invalidatePending(holder);
    }
}
