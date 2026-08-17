package br.rafaeros.fastrelax_api.features.chairs;

import java.util.Objects;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairFilterDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairHeartbeatRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.SaveChairRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chairs")
@RequiredArgsConstructor
@Tag(name = "Cadeiras")
public class ChairController {

    private final ChairService chairService;
    private final ChairCommandService chairCommandService;

    /**
     * Batida periódica do ESP32. Autenticada pelo token de dispositivo, não por
     * JWT — o hardware não faz login.
     */
    @PostMapping("/heartbeat")
    @Operation(summary = "Heartbeat do ESP32: informa que está online e atualiza o IP")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> heartbeat(
            @RequestBody @Valid ChairHeartbeatRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.registerHeartbeat(dto),
                "Heartbeat registrado"));
    }

    @GetMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Lista as cadeiras e o estado de conexão de cada uma")
    public ResponseEntity<ApiResponseDTO<Page<ChairResponseDTO>>> listAll(
            @ParameterObject ChairFilterDTO filter,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                chairService.findAll(filter, Objects.requireNonNull(pageable)),
                "Cadeiras listadas com sucesso"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Busca uma cadeira por id")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.findById(id), "Cadeira encontrada"));
    }

    @PostMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Cadastra uma cadeira pelo MAC address do ESP32")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> create(
            @RequestBody @Valid SaveChairRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(chairService.create(dto), "Cadeira cadastrada com sucesso"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Atualiza os dados de uma cadeira")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid SaveChairRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.update(id, dto),
                "Cadeira atualizada com sucesso"));
    }

    /**
     * Aciona o relé por alguns segundos, sem sessão.
     *
     * <p>
     * Restrito a ADMIN: liga fisicamente a cadeira e não passa por nenhuma das
     * regras de agendamento.
     */
    @PostMapping("/{id}/relay-test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liga o relé por alguns segundos para testar a instalação (somente ADMIN)")
    public ResponseEntity<ApiResponseDTO<Void>> testRelay(@PathVariable Long id,
            @RequestParam(name = "durationSeconds", defaultValue = "10") int durationSeconds) {
        chairCommandService.testRelay(chairService.findEntity(id), durationSeconds);
        return ResponseEntity.ok(ApiResponseDTO.success(
                "Relé acionado por " + durationSeconds + "s. Verifique a cadeira."));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Ativa ou desativa uma cadeira")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.toggleActive(id),
                "Status da cadeira alterado com sucesso"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Remove uma cadeira (soft delete)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        chairService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Cadeira removida com sucesso"));
    }
}
