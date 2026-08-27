package br.rafaeros.fastrelax_api.features.firmwares.dtos;

import br.rafaeros.fastrelax_api.features.firmwares.FirmwareFile;

/**
 * @param flashable se o arquivo pode ser gravado direto no ESP32. Só imagem
 *                  binária: o esptool não entende Intel HEX, então um
 *                  {@code .hex} fica disponível para download, mas não para
 *                  gravação — decidir isso aqui evita a interface oferecer um
 *                  botão que falharia no meio do processo.
 */
public record FirmwareFileResponseDTO(
    Long id,
    String fileName,
    long fileSize,
    String fileHash,
    String contentType,
    boolean flashable
) {
    public FirmwareFileResponseDTO(FirmwareFile entity) {
        this(
            entity.getId(),
            entity.getFileName(),
            entity.getFileSize(),
            entity.getFileHash(),
            entity.getContentType(),
            entity.getFileName() != null && entity.getFileName().toLowerCase().endsWith(".bin")
        );
    }
}
