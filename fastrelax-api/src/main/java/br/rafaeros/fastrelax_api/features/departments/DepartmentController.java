package br.rafaeros.fastrelax_api.features.departments;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.CreateDepartmentDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentFilterDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentRequestDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
@Tag(name = "Departamentos")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Lista os departamentos")
    public ResponseEntity<ApiResponseDTO<Page<DepartmentResponseDTO>>> listAll(
            @ParameterObject DepartmentFilterDTO filter,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<DepartmentResponseDTO> departments = departmentService.findAll(filter, Objects.requireNonNull(pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(departments, "Departamentos listados com sucesso"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Busca um departamento por id")
    public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> getById(@PathVariable Long id) {
        DepartmentResponseDTO department = departmentService.findById(id);
        return ResponseEntity.ok(ApiResponseDTO.success(department, "Departamento encontrado"));
    }

    @PostMapping
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Cadastra um departamento")
    public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> create(@RequestBody @Valid CreateDepartmentDTO dto) {
        DepartmentResponseDTO department = departmentService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(department, "Departamento criado com sucesso"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Atualiza um departamento")
    public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid DepartmentRequestDTO dto) {
        DepartmentResponseDTO department = departmentService.update(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.success(department, "Departamento atualizado com sucesso"));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Ativa ou desativa um departamento")
    public ResponseEntity<ApiResponseDTO<DepartmentResponseDTO>> toggleActive(@PathVariable Long id) {
        DepartmentResponseDTO department = departmentService.toggleActive(id);
        return ResponseEntity.ok(ApiResponseDTO.success(department, "Status do departamento alterado com sucesso"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Remove um departamento (soft delete)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        departmentService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Departamento deletado com sucesso"));
    }
}
