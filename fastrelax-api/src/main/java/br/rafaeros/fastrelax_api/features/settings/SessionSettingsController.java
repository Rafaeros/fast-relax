package br.rafaeros.fastrelax_api.features.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.settings.dtos.SessionSettingsResponseDTO;
import br.rafaeros.fastrelax_api.features.settings.dtos.UpdateSessionSettingsRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/settings/sessions")
@RequiredArgsConstructor
@Tag(name = "Configurações de sessão")
public class SessionSettingsController {

    private final SessionSettingsService settingsService;

    /** Leitura liberada a qualquer autenticado: o app precisa saber a duração para exibir os horários. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Duração, tolerância de início e antecedência máxima vigentes")
    public ResponseEntity<ApiResponseDTO<SessionSettingsResponseDTO>> get() {
        return ResponseEntity.ok(ApiResponseDTO.success(settingsService.get(), "Configuração encontrada"));
    }

    @PutMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Altera as configurações globais das sessões")
    public ResponseEntity<ApiResponseDTO<SessionSettingsResponseDTO>> update(
            @RequestBody @Valid UpdateSessionSettingsRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(settingsService.update(dto),
                "Configuração atualizada com sucesso"));
    }
}
