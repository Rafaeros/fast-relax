package br.rafaeros.fastrelax_api.features.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login de usuário ADMIN/RH por email e senha")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(@RequestBody @Valid LoginRequestDTO data,
            HttpServletRequest request) {
        LoginResponseDTO response = authService.login(data, clientKey(request));
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Login realizado com sucesso"));
    }

    @PostMapping("/collaborator/login")
    @Operation(summary = "Login de colaborador por CPF")
    public ResponseEntity<ApiResponseDTO<CollaboratorLoginResponseDTO>> collaboratorLogin(
            @RequestBody @Valid CollaboratorLoginRequestDTO data, HttpServletRequest request) {
        CollaboratorLoginResponseDTO response = authService.collaboratorLogin(data, clientKey(request));
        return ResponseEntity.ok(ApiResponseDTO.success(response, "Login realizado com sucesso"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refresh token por um novo par de tokens")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> refresh(
            @RequestBody @Valid RefreshTokenRequestDTO data) {
        return ResponseEntity.ok(ApiResponseDTO.success(authService.refresh(data), "Token renovado com sucesso"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalida o refresh token do dispositivo atual")
    public ResponseEntity<ApiResponseDTO<Void>> logout(@RequestBody @Valid RefreshTokenRequestDTO data) {
        authService.logout(data);
        return ResponseEntity.ok(ApiResponseDTO.success("Logout realizado com sucesso"));
    }

    /**
     * Chave do rate limit. Prefere o IP original quando há proxy na frente —
     * sem isso, todas as requisições compartilhariam o IP do balanceador e um
     * único cliente abusivo bloquearia todo mundo.
     */
    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
