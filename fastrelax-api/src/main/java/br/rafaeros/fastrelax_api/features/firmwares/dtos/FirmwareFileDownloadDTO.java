package br.rafaeros.fastrelax_api.features.firmwares.dtos;

/**
 * Binário pronto para ser devolvido ao cliente.
 *
 * @param fileName    nome original, usado no Content-Disposition
 * @param contentType tipo declarado no upload
 * @param content     bytes gravados
 */
public record FirmwareFileDownloadDTO(String fileName, String contentType, byte[] content) {
}
