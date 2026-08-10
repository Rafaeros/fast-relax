package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import br.rafaeros.fastrelax_api.features.collaborators.dtos.AvailableSlotsResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/collaborators/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessões de descanso")
public class CollaboratorSessionController {

    private final CollaboratorSessionService sessionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista as sessões; o colaborador vê apenas as próprias")
    public ResponseEntity<ApiResponseDTO<Page<CollaboratorSessionResponseDTO>>> listAll(
            @ParameterObject CollaboratorSessionFilterDTO filter,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        // O service restringe a listagem ao próprio colaborador quando não é ADMIN/RH.
        Page<CollaboratorSessionResponseDTO> sessions = sessionService.findAll(filter,
                Objects.requireNonNull(pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(sessions, "Sessões listadas com sucesso"));
    }

    /**
     * Horários ainda livres num período. Colaborador logado pode omitir
     * {@code collaboratorId}; sem {@code from}/{@code to} devolve de hoje até o
     * limite de antecedência configurado.
     */
    @GetMapping("/available-slots")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista os horários livres dentro do período informado")
    public ResponseEntity<ApiResponseDTO<AvailableSlotsResponseDTO>> availableSlots(
            @RequestParam(required = false) Long collaboratorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AvailableSlotsResponseDTO slots = sessionService.findAvailableSlots(collaboratorId, from, to);
        return ResponseEntity.ok(ApiResponseDTO.success(slots, "Horários disponíveis listados com sucesso"));
    }

    /**
     * Sessão ativa do colaborador logado. Devolve {@code data: null} quando não há
     * nenhuma, para a tela inicial decidir entre "agendar" e "iniciar".
     */
    @GetMapping("/me/current")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sessão ativa do colaborador logado; 204 quando não há nenhuma")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> myCurrentSession() {
        return sessionService.findMyCurrentSession()
                .map(session -> ResponseEntity.ok(ApiResponseDTO.success(session, "Sessão ativa encontrada")))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponseDTO.success("Nenhuma sessão ativa no momento")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca uma sessão por id")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(sessionService.findById(id), "Sessão encontrada"));
    }

    @PostMapping
    @PreAuthorize("@collaboratorSecurity.canAccessCollaborator(#dto.collaboratorId())")
    @Operation(summary = "Agenda uma sessão dentro do horário de almoço")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> create(
            @RequestBody @Valid CollaboratorSessionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(sessionService.create(dto), "Sessão agendada com sucesso"));
    }

    /** Reagenda data e horário. Para mudar de estado use as transições abaixo. */
    @PutMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.canAccessCollaborator(#dto.collaboratorId())")
    @Operation(summary = "Reagenda data e horário de uma sessão ainda agendada")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid CollaboratorSessionDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(sessionService.update(id, dto),
                "Sessão reagendada com sucesso"));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Inicia a sessão; só dentro do dia e da janela agendada")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> start(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(sessionService.start(id), "Sessão iniciada"));
    }

    @PatchMapping("/{id}/finish")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Finaliza a sessão em andamento")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> finish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(sessionService.finish(id), "Sessão finalizada"));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela a sessão e libera o horário")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(sessionService.cancel(id), "Sessão cancelada"));
    }

    /** Mantido por compatibilidade: cancelar é a única "exclusão" que uma sessão tem. */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela a sessão (equivalente ao cancelamento)")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(sessionService.cancel(id), "Sessão cancelada"));
    }
}
