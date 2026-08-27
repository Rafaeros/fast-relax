package br.rafaeros.fastrelax_api.features.firmwares;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.FirmwareFileDownloadDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.FirmwareFileResponseDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.FirmwareResponseDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.SaveFirmwareRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/firmwares")
@RequiredArgsConstructor
@Tag(name = "Firmwares")
public class FirmwareController {

    private final FirmwareService firmwareService;

    /** Leitura aberta a qualquer autenticado: a empresa precisa saber o que roda nas cadeiras dela. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista as versões de firmware publicadas")
    public ResponseEntity<ApiResponseDTO<Page<FirmwareResponseDTO>>> listAll(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success(firmwareService.findAll(pageable),
                "Firmwares listados com sucesso"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca uma versão de firmware por id")
    public ResponseEntity<ApiResponseDTO<FirmwareResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(firmwareService.findById(id), "Firmware encontrado"));
    }

    @PostMapping
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Publica uma versão de firmware")
    public ResponseEntity<ApiResponseDTO<FirmwareResponseDTO>> create(
            @RequestBody @Valid SaveFirmwareRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(firmwareService.create(dto), "Firmware publicado com sucesso"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Atualiza uma versão de firmware")
    public ResponseEntity<ApiResponseDTO<FirmwareResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid SaveFirmwareRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(firmwareService.update(id, dto),
                "Firmware atualizado com sucesso"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Retira uma versão de firmware do catálogo (soft delete)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        firmwareService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Firmware removido com sucesso"));
    }

    @PostMapping(path = "/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Anexa um binário (.bin ou .hex) à versão; tamanho e SHA-256 são calculados aqui")
    public ResponseEntity<ApiResponseDTO<FirmwareFileResponseDTO>> attachFile(@PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponseDTO.success(firmwareService.attachFile(id, file), "Arquivo anexado com sucesso"));
    }

    /**
     * Devolve os bytes.
     *
     * <p>
     * Fora do envelope {@code ApiResponseDTO} de propósito: quem consome é o
     * download do navegador e o gravador via Web Serial, e os dois querem o
     * arquivo cru — embrulhar em JSON obrigaria a decodificar base64 dos dois
     * lados sem ganho nenhum.
     */
    @GetMapping("/{id}/files/{fileId}/content")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Baixa o binário do firmware")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id, @PathVariable Long fileId) {
        FirmwareFileDownloadDTO download = firmwareService.downloadFile(id, fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/{id}/files/{fileId}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Remove um binário da versão")
    public ResponseEntity<ApiResponseDTO<Void>> deleteFile(@PathVariable Long id, @PathVariable Long fileId) {
        firmwareService.deleteFile(id, fileId);
        return ResponseEntity.ok(ApiResponseDTO.success("Arquivo removido com sucesso"));
    }
}
