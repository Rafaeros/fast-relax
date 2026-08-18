package br.rafaeros.fastrelax_api.features.notifications;

import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.notifications.dtos.NotificationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Central de notificações do colaborador.
 *
 * <p>
 * Existe porque push não é entrega garantida: o aviso fica aqui mesmo que
 * nenhum aparelho tenha recebido a mensagem.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificações")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista as notificações do colaborador logado, das mais recentes para as mais antigas")
    public ResponseEntity<ApiResponseDTO<Page<NotificationResponseDTO>>> listMine(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity
                .ok(ApiResponseDTO.success(notificationService.listMine(pageable), "Notificações listadas"));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Quantidade de notificações não lidas — alimenta o contador do sininho")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("count", notificationService.countMyUnread()), "Contagem calculada"));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marca uma notificação como lida")
    public ResponseEntity<ApiResponseDTO<NotificationResponseDTO>> markAsRead(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponseDTO.success(notificationService.markAsRead(id), "Notificação marcada como lida"));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marca todas as notificações do colaborador logado como lidas")
    public ResponseEntity<ApiResponseDTO<Map<String, Integer>>> markAllAsRead() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("updated", notificationService.markAllAsRead()), "Notificações marcadas como lidas"));
    }
}
