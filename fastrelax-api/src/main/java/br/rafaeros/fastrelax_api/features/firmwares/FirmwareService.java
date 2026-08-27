package br.rafaeros.fastrelax_api.features.firmwares;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.FirmwareResponseDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.FirmwareFileDownloadDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.FirmwareFileResponseDTO;
import br.rafaeros.fastrelax_api.features.firmwares.dtos.SaveFirmwareRequestDTO;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo de firmwares da Physical.
 *
 * <p>
 * Sem escopo de empresa em lugar nenhum, e é intencional: a versão é do produto,
 * não do cliente. Quem escreve é o SYSADMIN; as empresas leem para saber o que
 * roda nas cadeiras delas.
 */
@Service
@RequiredArgsConstructor
public class FirmwareService {

    private final FirmwareRepository firmwareRepository;
    private final FirmwareFileRepository fileRepository;
    private final FirmwareFileContentRepository contentRepository;

    public Page<FirmwareResponseDTO> findAll(@org.springframework.lang.NonNull Pageable pageable) {
        return firmwareRepository.findAll(Objects.requireNonNull(pageable)).map(FirmwareResponseDTO::new);
    }

    public FirmwareResponseDTO findById(Long id) {
        return new FirmwareResponseDTO(findEntityById(id));
    }

    @Transactional
    public FirmwareResponseDTO create(SaveFirmwareRequestDTO dto) {
        Firmware firmware = firmwareRepository.findByVersionIncludingDeleted(dto.version())
                .map(existing -> {
                    if (existing.getDeletedAt() == null) {
                        throw new BusinessException("Já existe um firmware com esta versão");
                    }
                    // A constraint UNIQUE de version não ignora soft delete: republicar
                    // uma versão retirada é reativar a linha, não inserir outra.
                    existing.setDeletedAt(null);
                    return existing;
                })
                .orElseGet(Firmware::new);

        applyFields(firmware, dto);
        return new FirmwareResponseDTO(firmwareRepository.save(firmware));
    }

    @Transactional
    public FirmwareResponseDTO update(Long id, SaveFirmwareRequestDTO dto) {
        Firmware firmware = findEntityById(id);

        firmwareRepository.findByVersionIncludingDeleted(dto.version())
                .filter(other -> !other.getId().equals(firmware.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Já existe um firmware com esta versão");
                });

        applyFields(firmware, dto);
        return new FirmwareResponseDTO(firmwareRepository.save(firmware));
    }

    /**
     * Remoção lógica. As cadeiras que apontam para esta versão continuam
     * apontando: apagar de verdade quebraria a FK e, pior, apagaria o registro do
     * que está gravado em campo — que é justamente o que o suporte precisa saber.
     */
    @Transactional
    public void softDelete(Long id) {
        Firmware firmware = findEntityById(id);
        firmware.setDeletedAt(LocalDateTime.now());
        firmwareRepository.save(firmware);
    }

    /**
     * Anexa um binário à versão.
     *
     * <p>
     * Tamanho e hash saem do que chegou, nunca do cliente: é o hash que o ESP32
     * usa para conferir o download antes de gravar na flash, e aceitar um valor
     * informado tornaria a checagem decorativa — bastaria o remetente calcular
     * errado para o dispositivo validar um arquivo corrompido.
     */
    @Transactional
    public FirmwareFileResponseDTO attachFile(Long firmwareId, MultipartFile upload) {
        Firmware firmware = findEntityById(firmwareId);

        if (upload == null || upload.isEmpty()) {
            throw new BusinessException("Selecione um arquivo de firmware");
        }

        String fileName = org.springframework.util.StringUtils.getFilename(upload.getOriginalFilename());
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("Arquivo sem nome");
        }

        String extension = fileName.toLowerCase();
        if (!extension.endsWith(".bin") && !extension.endsWith(".hex")) {
            throw new BusinessException("Formato não aceito. Envie um arquivo .bin ou .hex");
        }

        byte[] content;
        try {
            content = upload.getBytes();
        } catch (java.io.IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo enviado");
        }

        // Mesmo nome duas vezes na mesma versão seria ambíguo na hora de gravar.
        firmware.getFiles().stream()
                .filter(file -> file.getFileName().equalsIgnoreCase(fileName))
                .findFirst()
                .ifPresent(file -> {
                    throw new BusinessException("Esta versão já tem um arquivo chamado " + fileName);
                });

        FirmwareFile file = new FirmwareFile();
        file.setFileName(fileName);
        file.setFileSize(content.length);
        file.setFileHash(sha256Hex(content));
        file.setContentType(upload.getContentType());
        firmware.addFile(file);

        // Gravado pelo próprio repositório, e não pelo cascade do firmware: o
        // `save` do pai vira `merge` — ele já tem id —, e o merge de um filho
        // transiente cria uma cópia gerenciada. O id gerado ia parar na cópia, e
        // este `file` continuava com null, quebrando a chave do conteúdo abaixo.
        FirmwareFile saved = fileRepository.saveAndFlush(file);

        // Agora o id existe, e é ele que identifica os bytes.
        contentRepository.save(new FirmwareFileContent(saved.getId(), content));

        return new FirmwareFileResponseDTO(saved);
    }

    /** Bytes gravados, para download ou para a gravação pelo navegador. */
    public FirmwareFileDownloadDTO downloadFile(Long firmwareId, Long fileId) {
        FirmwareFile file = findFile(firmwareId, fileId);

        FirmwareFileContent content = contentRepository.findById(file.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "O arquivo está cadastrado mas o conteúdo não foi encontrado"));

        return new FirmwareFileDownloadDTO(file.getFileName(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                content.getContent());
    }

    /** Remove de verdade: o binário só existe para ser baixado ou gravado. */
    @Transactional
    public void deleteFile(Long firmwareId, Long fileId) {
        FirmwareFile file = findFile(firmwareId, fileId);
        Firmware firmware = file.getFirmware();

        // O flush é o que garante a ordem. No mesmo lote, o Hibernate executa a
        // remoção de órfão da coleção antes das exclusões de entidade — o arquivo
        // sairia primeiro, o banco levaria o conteúdo junto pelo CASCADE, e o
        // DELETE do conteúdo então afetaria zero linhas, o que o Hibernate trata
        // como estado obsoleto e transforma em erro.
        contentRepository.deleteById(file.getId());
        contentRepository.flush();

        firmware.getFiles().remove(file);
        firmwareRepository.save(firmware);
    }

    private FirmwareFile findFile(Long firmwareId, Long fileId) {
        return findEntityById(firmwareId).getFiles().stream()
                .filter(file -> file.getId().equals(Objects.requireNonNull(fileId)))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado nesta versão"));
    }

    /** SHA-256 em hexadecimal minúsculo — mesmo formato que o ESP32 compara. */
    private String sha256Hex(byte[] content) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório em toda JVM; chegar aqui é ambiente quebrado.
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }

    private void applyFields(Firmware firmware, SaveFirmwareRequestDTO dto) {
        firmware.setProductName(dto.productName());
        firmware.setVersion(dto.version());
        firmware.setReleaseNotes(dto.releaseNotes());
        firmware.setReleaseDate(dto.releaseDate());
        // Os arquivos não são tocados aqui de propósito: eles têm rotas próprias.
        // Substituir a lista a cada edição de metadado apagaria os binários.
    }

    private Firmware findEntityById(Long id) {
        return firmwareRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Firmware não encontrado"));
    }
}
