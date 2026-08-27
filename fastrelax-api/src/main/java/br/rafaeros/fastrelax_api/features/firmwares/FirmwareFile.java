package br.rafaeros.fastrelax_api.features.firmwares;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Binário de uma versão de firmware.
 *
 * <p>
 * Guarda os metadados; os bytes ficam em {@link FirmwareFileContent}, em tabela
 * própria, para que listar o catálogo não arraste o binário junto.
 *
 * <p>
 * Tamanho e hash são calculados no servidor a partir do que chegou, nunca
 * informados pelo cliente. O SHA-256 é o que permite ao ESP32 conferir o
 * download antes de gravar na flash: uma atualização corrompida no meio do
 * caminho vira um dispositivo que não liga mais, sem recuperação remota.
 */
@Entity
@Table(name = "firmware_files")
@NoArgsConstructor
@Getter
@Setter
public class FirmwareFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "firmware_id", nullable = false)
    private Firmware firmware;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    /** SHA-256 em hexadecimal — 64 caracteres. */
    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    /**
     * Tipo declarado no upload.
     *
     * <p>
     * Separa o que é gravável do que é só arquivo: o esptool trabalha com
     * imagem binária ({@code .bin}); Intel HEX ({@code .hex}) é formato de AVR e
     * não pode ser enviado ao ESP32 como está.
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
