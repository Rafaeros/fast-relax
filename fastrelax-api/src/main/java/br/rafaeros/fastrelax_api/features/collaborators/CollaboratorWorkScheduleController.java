package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorWorkScheduleDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorWorkScheduleFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorWorkScheduleResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.WeeklyScheduleRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/collaborators")
@RequiredArgsConstructor
@Tag(name = "Horários permitidos")
public class CollaboratorWorkScheduleController {

    private final CollaboratorWorkScheduleService scheduleService;

    @GetMapping("/schedules")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Lista os horários permitidos cadastrados")
    public ResponseEntity<ApiResponseDTO<Page<CollaboratorWorkScheduleResponseDTO>>> listAll(
            @ParameterObject CollaboratorWorkScheduleFilterDTO filter,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<CollaboratorWorkScheduleResponseDTO> schedules = scheduleService
                .findAll(filter, Objects.requireNonNull(pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(schedules, "Horários listados com sucesso"));
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca um horário permitido por id")
    public ResponseEntity<ApiResponseDTO<CollaboratorWorkScheduleResponseDTO>> getById(@PathVariable Long id) {
        // O service checa se o colaborador logado é o dono do horário.
        return ResponseEntity.ok(ApiResponseDTO.success(scheduleService.findById(id), "Horário encontrado"));
    }

    /** Semana do colaborador logado. Evita o app ter que guardar o próprio id. */
    @GetMapping("/me/schedules")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Semana de horários permitidos do colaborador logado")
    public ResponseEntity<ApiResponseDTO<List<CollaboratorWorkScheduleResponseDTO>>> myWeeklySchedule() {
        List<CollaboratorWorkScheduleResponseDTO> schedules = scheduleService.findMyWeeklySchedule();
        return ResponseEntity.ok(ApiResponseDTO.success(schedules, "Horários da semana listados com sucesso"));
    }

    @GetMapping("/{collaboratorId}/schedules")
    @PreAuthorize("@access.operatesCompany()"
            + " or @access.canAccessCollaborator(#collaboratorId)")
    @Operation(summary = "Semana de horários permitidos de um colaborador")
    public ResponseEntity<ApiResponseDTO<List<CollaboratorWorkScheduleResponseDTO>>> getWeeklySchedule(
            @PathVariable Long collaboratorId) {
        List<CollaboratorWorkScheduleResponseDTO> schedules = scheduleService.findWeeklySchedule(collaboratorId);
        return ResponseEntity.ok(ApiResponseDTO.success(schedules, "Horários da semana listados com sucesso"));
    }

    /** Define a semana inteira de uma vez: dias ausentes do corpo são desativados. */
    @PutMapping("/{collaboratorId}/schedules")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Substitui a semana inteira; dias omitidos são desativados")
    public ResponseEntity<ApiResponseDTO<List<CollaboratorWorkScheduleResponseDTO>>> replaceWeeklySchedule(
            @PathVariable Long collaboratorId,
            @RequestBody @Valid WeeklyScheduleRequestDTO dto) {
        List<CollaboratorWorkScheduleResponseDTO> schedules = scheduleService
                .replaceWeeklySchedule(collaboratorId, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(schedules, "Horários da semana salvos com sucesso"));
    }

    @PostMapping("/schedules")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Cadastra o horário permitido de um único dia")
    public ResponseEntity<ApiResponseDTO<CollaboratorWorkScheduleResponseDTO>> create(
            @RequestBody @Valid CollaboratorWorkScheduleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(scheduleService.create(dto), "Horário criado com sucesso"));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Atualiza o horário permitido de um único dia")
    public ResponseEntity<ApiResponseDTO<CollaboratorWorkScheduleResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid CollaboratorWorkScheduleDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(scheduleService.update(id, dto),
                "Horário atualizado com sucesso"));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Remove o horário permitido de um dia")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        scheduleService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Horário deletado com sucesso"));
    }
}
