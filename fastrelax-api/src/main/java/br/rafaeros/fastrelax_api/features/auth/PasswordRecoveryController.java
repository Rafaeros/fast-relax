package br.rafaeros.fastrelax_api.features.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.core.security.LoginRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Recuperação de senha, para painel e app do colaborador.
 *
 * <p>
 * Rotas públicas: quem chega aqui perdeu o acesso, então exigir autenticação
 * seria contraditório. É justamente por serem públicas que carregam o mesmo
 * limite de tentativas do login — sem ele, dispararia-se e-mail em massa a
 * partir de uma lista de endereços, e o domínio da Physical acabaria marcado
 * como remetente de spam.
 */
@RestController
@RequestMapping("/auth/password")
@RequiredArgsConstructor
@Tag(name = "Recuperação de senha")
public class PasswordRecoveryController {

    private final PasswordRecoveryService recoveryService;
    private final LoginRateLimiter rateLimiter;

    @PostMapping("/forgot")
    @Operation(summary = "Envia o link de redefinição para um usuário do painel")
    public ResponseEntity<ApiResponseDTO<Void>> forgot(
            @RequestBody @Valid ForgotPasswordRequestDTO data, HttpServletRequest request) {
        rateLimiter.checkAndRegister(clientKey(request));
        recoveryService.requestForUser(data.email());

        // Resposta idêntica para conta existente e inexistente: ver
        // PasswordRecoveryService.
        return ResponseEntity.ok(
                ApiResponseDTO.success(PasswordRecoveryService.GENERIC_REQUEST_MESSAGE));
    }

    @PostMapping("/collaborator/forgot")
    @Operation(summary = "Envia o link de redefinição para um colaborador (slug da empresa + e-mail)")
    public ResponseEntity<ApiResponseDTO<Void>> collaboratorForgot(
            @RequestBody @Valid CollaboratorForgotPasswordRequestDTO data,
            HttpServletRequest request) {
        rateLimiter.checkAndRegister(clientKey(request));
        recoveryService.requestForCollaborator(data.companySlug(), data.email());

        return ResponseEntity.ok(
                ApiResponseDTO.success(PasswordRecoveryService.GENERIC_REQUEST_MESSAGE));
    }

    /**
     * Quem é o dono do link, sem gastá-lo.
     *
     * <p>
     * Consumir aqui invalidaria o token só por alguém abrir a página — e o
     * preview de link de qualquer aplicativo de mensagem já queimaria o convite
     * antes de a pessoa clicar.
     */
    @GetMapping("/token")
    @Operation(summary = "Valida o link e devolve o mínimo para a tela se apresentar")
    public ResponseEntity<ApiResponseDTO<RecoveryTargetResponseDTO>> describe(
            @RequestParam String token) {
        return recoveryService.describe(token)
                .map(target -> ResponseEntity.ok(ApiResponseDTO.success(
                        RecoveryTargetResponseDTO.from(target), "Link válido")))
                .orElseGet(() -> ResponseEntity.ok(ApiResponseDTO.success(
                        (RecoveryTargetResponseDTO) null,
                        "Link inválido ou expirado. Peça um novo para redefinir sua senha.")));
    }

    @PostMapping("/reset")
    @Operation(summary = "Define a senha a partir do link recebido por e-mail")
    public ResponseEntity<ApiResponseDTO<Void>> reset(
            @RequestBody @Valid ResetPasswordRequestDTO data, HttpServletRequest request) {
        rateLimiter.checkAndRegister(clientKey(request));
        recoveryService.completeWithToken(data.token(), data.newPassword(), data.confirmNewPassword());
        rateLimiter.reset(clientKey(request));

        return ResponseEntity.ok(ApiResponseDTO.success(
                "Senha definida com sucesso. Entre com a nova senha."));
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
