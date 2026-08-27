package br.rafaeros.fastrelax_api.features.firmwares.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import br.rafaeros.fastrelax_api.features.firmwares.Firmware;

public record FirmwareResponseDTO(
    Long id,
    String productName,
    String version,
    String releaseNotes,
    LocalDate releaseDate,
    List<FirmwareFileResponseDTO> files,
    LocalDateTime createdAt
) {
    public FirmwareResponseDTO(Firmware entity) {
        this(entity.getId(), entity.getProductName(), entity.getVersion(), entity.getReleaseNotes(),
                entity.getReleaseDate(),
                entity.getFiles().stream().map(FirmwareFileResponseDTO::new).toList(),
                entity.getCreatedAt());
    }
}
