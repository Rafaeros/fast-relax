package br.rafaeros.fastrelax_api.features.notifications;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.notifications.dtos.DeviceTokenResponseDTO;
import br.rafaeros.fastrelax_api.features.notifications.dtos.RegisterDeviceTokenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications/devices")
@RequiredArgsConstructor
@Tag(name = "Notificações")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registra o token de push do aparelho do colaborador logado")
    public ResponseEntity<ApiResponseDTO<DeviceTokenResponseDTO>> register(
            @RequestBody @Valid RegisterDeviceTokenDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(deviceTokenService.register(dto), "Dispositivo registrado"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista os aparelhos ativos do colaborador logado")
    public ResponseEntity<ApiResponseDTO<List<DeviceTokenResponseDTO>>> listMine() {
        return ResponseEntity.ok(ApiResponseDTO.success(deviceTokenService.listMine(), "Dispositivos listados"));
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Desativa o token do aparelho (logout do dispositivo)")
    public ResponseEntity<ApiResponseDTO<Void>> unregister(@RequestParam("token") String token) {
        deviceTokenService.unregister(token);
        return ResponseEntity.ok(ApiResponseDTO.success("Dispositivo removido"));
    }
}
