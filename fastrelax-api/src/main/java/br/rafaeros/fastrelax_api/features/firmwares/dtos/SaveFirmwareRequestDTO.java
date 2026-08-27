package br.rafaeros.fastrelax_api.features.firmwares.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Publicação de uma versão de firmware pela equipe da plataforma.
 *
 * <p>
 * Só metadados. Os binários entram e saem por rotas próprias
 * ({@code POST|DELETE /firmwares/{id}/files}): tratá-los aqui faria cada edição
 * de nota de versão substituir a lista inteira de arquivos — e, agora que a
 * lista carrega bytes, apagar o que estava anexado.
 */
public record SaveFirmwareRequestDTO(
    @NotBlank(message = "O nome do produto é obrigatório")
    @Size(max = 100, message = "O nome do produto deve ter no máximo 100 caracteres")
    String productName,

    @NotBlank(message = "A versão é obrigatória")
    @Size(max = 50, message = "A versão deve ter no máximo 50 caracteres")
    String version,

    String releaseNotes,

    @NotNull(message = "A data de publicação é obrigatória")
    LocalDate releaseDate
) {}
