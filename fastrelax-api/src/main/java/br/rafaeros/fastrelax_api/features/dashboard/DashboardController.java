package br.rafaeros.fastrelax_api.features.dashboard;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.dashboard.dtos.DashboardResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/sessions")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Métricas de sessões no período, para o painel do RH")
    public ResponseEntity<ApiResponseDTO<DashboardResponseDTO>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DashboardResponseDTO summary = dashboardService.summary(from, to);
        return ResponseEntity.ok(ApiResponseDTO.success(summary, "Métricas carregadas com sucesso"));
    }
}
