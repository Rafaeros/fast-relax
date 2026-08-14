package br.rafaeros.fastrelax_api.features.imports;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.imports.dtos.ImportResultDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/imports/collaborators")
@RequiredArgsConstructor
@Tag(name = "Importação e exportação")
public class CollaboratorImportController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final CollaboratorImportService importService;
    private final CollaboratorExportService exportService;

    /** Modelo .xlsx em branco, com o cabeçalho esperado e uma linha de exemplo. */
    @GetMapping("/template")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Baixa a planilha modelo com o cabeçalho esperado")
    public ResponseEntity<byte[]> template() {
        return xlsx(exportService.template(), "modelo-colaboradores.xlsx");
    }

    /**
     * Exporta os colaboradores no mesmo layout da importação, para editar em massa
     * e reenviar. Sem colaboradores, devolve só o cabeçalho.
     */
    @GetMapping("/export")
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Exporta os colaboradores no mesmo layout da importação")
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "onlyActive", defaultValue = "false") boolean onlyActive) {
        return xlsx(exportService.export(onlyActive), "colaboradores.xlsx");
    }

    /**
     * Importa colaboradores e o horário permitido de segunda a sexta.
     * Envie como {@code multipart/form-data} no campo {@code file}.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@collaboratorSecurity.hasAdminOrRhAccess()")
    @Operation(summary = "Importa colaboradores, departamentos e horários permitidos a partir de .xlsx")
    public ResponseEntity<ApiResponseDTO<ImportResultDTO>> importCollaborators(
            @RequestParam("file") MultipartFile file) {
        ImportResultDTO result = importService.importFrom(file);
        String message = result.failed() == 0
                ? "Importação concluída com sucesso"
                : "Importação concluída com " + result.failed() + " linha(s) com erro";
        return ResponseEntity.ok(ApiResponseDTO.success(result, message));
    }

    private ResponseEntity<byte[]> xlsx(byte[] content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(content);
    }
}
