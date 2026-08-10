package br.rafaeros.fastrelax_api.features.users;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import br.rafaeros.fastrelax_api.features.users.dtos.CreateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.CreatedUserResponseDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UpdateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    /**
     * A senha temporária é gerada aqui e devolvida em claro uma única vez — o banco
     * guarda só o hash, então não há como recuperá-la depois.
     */
    @Transactional
    public CreatedUserResponseDTO createUser(CreateUserRequestDTO user) {
        if (userRepository.existsByEmail(user.email())) {
            throw new BusinessException("Email já está em uso!");
        }
        String temporaryPassword = TemporaryPasswordGenerator.generate();

        User newUser = new User();
        newUser.setName(user.name());
        newUser.setEmail(user.email());
        newUser.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        newUser.setRole(user.role());
        newUser.setMustChangePassword(true);

        User savedUser = userRepository.save(newUser);
        return new CreatedUserResponseDTO(new UserResponseDTO(savedUser), temporaryPassword);
    }

    public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAll(Objects.requireNonNull(pageable)).map(UserResponseDTO::new);
    }

    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
        return new UserResponseDTO(user);
    }

    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO user) {
        User existingUser = userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new BusinessException("Email já está em uso!");
            }
            existingUser.setEmail(user.getEmail());
        }
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        User updatedUser = userRepository.save(Objects.requireNonNull(existingUser));
        return new UserResponseDTO(updatedUser);
    }

    /** Dados do usuário ADMIN/RH logado. */
    public UserResponseDTO findAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User logged)) {
            throw new AccessDeniedException("Rota disponível apenas para usuários ADMIN/RH autenticados");
        }
        return findUserById(logged.getId());
    }

    @Transactional
    public UserResponseDTO toggleActive(Long id) {
        User user = findEntity(id);
        user.setActive(!user.isActive());
        // Desativar precisa cortar as sessões abertas, senão o token continua
        // valendo até expirar.
        if (!user.isActive()) {
            refreshTokenService.revokeAllFor(RefreshToken.SubjectType.USER, user.getId());
        }
        return new UserResponseDTO(userRepository.save(user));
    }

    @Transactional
    public void softDelete(Long id) {
        User user = findEntity(id);
        user.setActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllFor(RefreshToken.SubjectType.USER, user.getId());
    }

    private User findEntity(Long id) {
        return userRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }
}
