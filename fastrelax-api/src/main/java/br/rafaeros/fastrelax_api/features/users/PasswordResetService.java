package br.rafaeros.fastrelax_api.features.users;

import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import br.rafaeros.fastrelax_api.features.auth.RefreshTokenService;
import br.rafaeros.fastrelax_api.core.util.TemporaryPasswordGenerator;
import br.rafaeros.fastrelax_api.features.users.dtos.ChangePasswordRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.FirstAccessPasswordRequestDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /** Troca da própria senha, conferindo a atual. */
    @Transactional
    public void changeOwnPassword(ChangePasswordRequestDTO dto) {
        User user = requireLoggedUser();

        if (!passwordEncoder.matches(dto.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta");
        }
        requireMatchingConfirmation(dto.newPassword(), dto.confirmNewPassword());
        requireDifferentFromCurrent(user, dto.newPassword());

        applyNewPassword(user, dto.newPassword());
    }

    /**
     * Primeiro acesso: o usuário define a própria senha e o sistema libera o
     * restante da API para ele.
     */
    @Transactional
    public void defineFirstAccessPassword(FirstAccessPasswordRequestDTO dto) {
        User user = requireLoggedUser();

        if (!user.isMustChangePassword()) {
            throw new BusinessException(
                    "Sua senha já foi definida. Use a troca de senha para alterá-la.");
        }
        requireMatchingConfirmation(dto.newPassword(), dto.confirmNewPassword());
        requireDifferentFromCurrent(user, dto.newPassword());

        user.setMustChangePassword(false);
        applyNewPassword(user, dto.newPassword());
    }

    /**
     * Redefinição por ADMIN, pelo mesmo princípio do cadastro: quem redefine não
     * escolhe a senha de outra pessoa. Gera uma temporária, devolve uma única vez
     * e obriga o dono a trocá-la no próximo acesso.
     *
     * @return a senha temporária em claro, para o ADMIN repassar
     */
    @Transactional
    public String resetPassword(Long userId) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));

        String temporaryPassword = TemporaryPasswordGenerator.generate();
        user.setMustChangePassword(true);
        applyNewPassword(user, temporaryPassword);
        return temporaryPassword;
    }

    private void requireMatchingConfirmation(String newPassword, String confirmation) {
        if (!newPassword.equals(confirmation)) {
            throw new BusinessException("A confirmação não confere com a nova senha");
        }
    }

    private void requireDifferentFromCurrent(User user, String newPassword) {
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("A nova senha deve ser diferente da atual");
        }
    }

    /**
     * Troca a senha e derruba as sessões abertas: se a troca foi motivada por
     * suspeita de acesso indevido, manter os refresh tokens válidos anularia o
     * efeito da medida.
     */
    private void applyNewPassword(User user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllFor(RefreshToken.SubjectType.USER, user.getId());
    }

    private User requireLoggedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User logged)) {
            throw new AccessDeniedException("Rota disponível apenas para usuários ADMIN/RH autenticados");
        }
        return userRepository.findById(Objects.requireNonNull(logged.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }
}
