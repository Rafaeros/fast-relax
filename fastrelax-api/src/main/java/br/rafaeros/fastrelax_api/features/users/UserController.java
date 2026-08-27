package br.rafaeros.fastrelax_api.features.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.ChangePasswordRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.CreateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.CreatedUserResponseDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.FirstAccessPasswordRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.TemporaryPasswordResponseDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UpdateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@PreAuthorize("@access.isPlatformTeam() or @access.operatesCompany()")
@RequiredArgsConstructor
@Tag(name = "Usuários (ADMIN/RH)")
public class UserController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping
    @Operation(summary = "Cria um usuário ADMIN/RH e devolve a senha temporária gerada")
    public ResponseEntity<ApiResponseDTO<CreatedUserResponseDTO>> createUser(
            @RequestBody @Valid CreateUserRequestDTO requestData) {
        CreatedUserResponseDTO savedUser = userService.createUser(requestData);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(savedUser,
                        "Usuário criado! Repasse a senha temporária: ela não será exibida novamente."));
    }

    @GetMapping
    @Operation(summary = "Lista os usuários")
    public ResponseEntity<ApiResponseDTO<Page<UserResponseDTO>>> getAllUsers(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<UserResponseDTO> users = userService.findAllUsers(pageable);
        return ResponseEntity.ok(ApiResponseDTO.success(users, "Usuários listados com sucesso!"));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> me() {
        return ResponseEntity.ok(ApiResponseDTO.success(userService.findAuthenticated(),
                "Usuário encontrado com sucesso!"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário por id")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.findUserById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(user, "Usuário encontrado com sucesso!"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza nome e email")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequestDTO requestData) {
        UserResponseDTO updatedUser = userService.updateUser(id, requestData);
        return ResponseEntity.ok(ApiResponseDTO.success(updatedUser, "Usuário atualizado com sucesso!"));
    }

    @PostMapping("/me/first-access-password")
    @Operation(summary = "Define a senha no primeiro acesso e libera o restante da API")
    public ResponseEntity<ApiResponseDTO<Void>> defineFirstAccessPassword(
            @RequestBody @Valid FirstAccessPasswordRequestDTO requestData) {
        passwordResetService.defineFirstAccessPassword(requestData);
        return ResponseEntity.ok(ApiResponseDTO.success("Senha definida com sucesso! Faça login novamente."));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Troca a própria senha; derruba as sessões abertas")
    public ResponseEntity<ApiResponseDTO<Void>> changeOwnPassword(
            @RequestBody @Valid ChangePasswordRequestDTO requestData) {
        passwordResetService.changeOwnPassword(requestData);
        return ResponseEntity.ok(ApiResponseDTO.success("Senha alterada com sucesso! Faça login novamente."));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("@access.isPlatformTeam() or @access.administersCompany()")
    @Operation(summary = "Gera nova senha temporária para outro usuário (somente ADMIN)")
    public ResponseEntity<ApiResponseDTO<TemporaryPasswordResponseDTO>> resetPassword(@PathVariable Long id) {
        String temporaryPassword = passwordResetService.resetPassword(id);
        return ResponseEntity.ok(ApiResponseDTO.success(new TemporaryPasswordResponseDTO(temporaryPassword),
                "Senha redefinida! Repasse a senha temporária: ela não será exibida novamente."));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@access.isPlatformTeam() or @access.administersCompany()")
    @Operation(summary = "Ativa ou desativa um usuário (somente ADMIN)")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(userService.toggleActive(id),
                "Status do usuário alterado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam() or @access.administersCompany()")
    @Operation(summary = "Remove um usuário (soft delete, somente ADMIN)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        userService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Usuário removido com sucesso!"));
    }
}
