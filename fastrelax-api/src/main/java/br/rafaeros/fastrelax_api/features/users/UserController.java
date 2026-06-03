package br.rafaeros.fastrelax_api.features.users;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.CreateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping

    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> createUser(
            @RequestBody @Valid CreateUserRequestDTO requestData) {
        UserResponseDTO savedUser = userService.createUser(requestData);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(savedUser, "Usuário criado com sucesso!"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<Page<UserResponseDTO>>> getAllUsers(@PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponseDTO> users = userService.findAllUsers(pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(users, "Usuários listados com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.findUserById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(user, "Usuário encontrado com sucesso!"));
    }

}
