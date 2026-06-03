package br.rafaeros.fastrelax_api.features.users;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.users.dtos.CreateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO createUser(CreateUserRequestDTO user) {
        if (userRepository.existsByEmail(user.email())) {
            throw new BusinessException("Email já está em uso!");
        }
        User newUser = new User();
        newUser.setName(user.name());
        newUser.setEmail(user.email());
        newUser.setPasswordHash(passwordEncoder.encode(user.password()));
        newUser.setRole(user.role());
        User savedUser = userRepository.save(newUser);
        return new UserResponseDTO(savedUser);
    }

    public Page<UserResponseDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDTO::new);
    }

    public UserResponseDTO findUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
        return new UserResponseDTO(user);
    }

}
