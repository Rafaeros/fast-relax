package br.rafaeros.fastrelax_api.features.users;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.security.CredentialService;
import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.features.users.dtos.ChangePasswordRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.FirstAccessPasswordRequestDTO;
import lombok.RequiredArgsConstructor;

/**
 * Senha do usuário do painel.
 *
 * <p>
 * As regras vivem no {@link CredentialService}, compartilhadas com o
 * colaborador. O que sobra aqui é resolver de quem é a credencial e gravar.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CredentialService credentialService;

    /** Troca da própria senha, conferindo a atual. */
    @Transactional
    public void changeOwnPassword(ChangePasswordRequestDTO dto) {
        User user = requireLoggedUser();
        credentialService.changePassword(user, dto.currentPassword(), dto.newPassword(), dto.confirmNewPassword());
        userRepository.save(user);
    }

    /**
     * Primeiro acesso: o usuário define a própria senha e o sistema libera o
     * restante da API para ele.
     */
    @Transactional
    public void defineFirstAccessPassword(FirstAccessPasswordRequestDTO dto) {
        User user = requireLoggedUser();
        credentialService.defineFirstAccessPassword(user, dto.newPassword(), dto.confirmNewPassword());
        userRepository.save(user);
    }

    /**
     * Redefinição por quem administra. Passa pelo {@link UserService} para herdar
     * a mesma checagem de alcance do cadastro — o gestor de um cliente não
     * redefine a senha de usuário de outro.
     *
     * @return a senha temporária em claro, para quem administra repassar
     */
    @Transactional
    public String resetPassword(Long userId) {
        User user = userService.findManageableEntity(Objects.requireNonNull(userId));
        String temporaryPassword = credentialService.resetToTemporaryPassword(user);
        userRepository.save(user);
        return temporaryPassword;
    }

    private User requireLoggedUser() {
        Long id = Principals.requireUser().getId();
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }
}
