package br.rafaeros.fastrelax_api.features.collaborators;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CreateCollaboratorRequestDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.UpdateCollaboratorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/collaborators")
@RequiredArgsConstructor
@Tag(name = "Colaboradores")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    @GetMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Lista os colaboradores com filtros e paginação")
    public ResponseEntity<ApiResponseDTO<Page<CollaboratorResponseDTO>>> listAll(
            @ParameterObject CollaboratorFilterDTO filter,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<CollaboratorResponseDTO> collaborators = collaboratorService
                .findAll(filter, Objects.requireNonNull(pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(collaborators, "Colaboradores listados com sucesso"));
    }

    /** Dados do colaborador logado, sem precisar informar o id. */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Dados do colaborador logado")
    public ResponseEntity<ApiResponseDTO<CollaboratorResponseDTO>> me() {
        return ResponseEntity.ok(ApiResponseDTO.success(collaboratorService.findAuthenticated(),
                "Colaborador encontrado"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.canAccessCollaborator(#id)")
    @Operation(summary = "Busca um colaborador por id")
    public ResponseEntity<ApiResponseDTO<CollaboratorResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(collaboratorService.findById(id), "Colaborador encontrado"));
    }

    @PostMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Cadastra um colaborador")
    public ResponseEntity<ApiResponseDTO<CollaboratorResponseDTO>> create(
            @RequestBody @Valid CreateCollaboratorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(collaboratorService.create(dto), "Colaborador criado com sucesso"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Atualiza um colaborador; o CPF é opcional")
    public ResponseEntity<ApiResponseDTO<CollaboratorResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid UpdateCollaboratorDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(collaboratorService.update(id, dto),
                "Colaborador atualizado com sucesso"));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Ativa ou desativa; ao desativar, derruba as sessões abertas")
    public ResponseEntity<ApiResponseDTO<CollaboratorResponseDTO>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(collaboratorService.toggleActive(id),
                "Status do colaborador alterado com sucesso"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Remove um colaborador (soft delete)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        collaboratorService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Colaborador deletado com sucesso"));
    }
}
