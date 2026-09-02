package br.rafaeros.fastrelax_api.features.companies;

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
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.companies.dtos.CompanyResponseDTO;
import br.rafaeros.fastrelax_api.features.companies.dtos.SaveCompanyRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * CRUD de empresas é exclusivo da equipe da plataforma — o {@code SecurityConfig}
 * já barra {@code /companies/**} para os demais papéis, e os
 * {@code @PreAuthorize} abaixo repetem a regra junto do caso de uso, para quem
 * lê o controller não depender de lembrar da configuração.
 *
 * <p>
 * A única exceção é {@code /companies/me}: leitura da própria empresa, para o
 * RH/admin do cliente ver o slug que os colaboradores usam para entrar.
 */
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@Tag(name = "Empresas")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/me")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Dados da própria empresa, incluindo o slug de login")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> getMine() {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.findMine(), "Empresa encontrada"));
    }

    @GetMapping
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Lista as empresas clientes")
    public ResponseEntity<ApiResponseDTO<Page<CompanyResponseDTO>>> listAll(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.findAll(pageable),
                "Empresas listadas com sucesso"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Busca uma empresa por id")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.findById(id), "Empresa encontrada"));
    }

    @PostMapping
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Cadastra uma empresa cliente")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> create(
            @RequestBody @Valid SaveCompanyRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(companyService.create(dto), "Empresa criada com sucesso"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Atualiza uma empresa")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid SaveCompanyRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.update(id, dto),
                "Empresa atualizada com sucesso"));
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Suspende ou reativa o contrato de uma empresa")
    public ResponseEntity<ApiResponseDTO<CompanyResponseDTO>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(companyService.toggleActive(id),
                "Status da empresa alterado com sucesso"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Remove uma empresa (soft delete)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        companyService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Empresa removida com sucesso"));
    }
}
