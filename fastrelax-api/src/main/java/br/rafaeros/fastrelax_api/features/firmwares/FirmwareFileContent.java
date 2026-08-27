package br.rafaeros.fastrelax_api.features.firmwares;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Bytes de um binário de firmware.
 *
 * <p>
 * Separado de {@link FirmwareFile} porque o Hibernate lista todas as colunas
 * mapeadas em cada SELECT: com o bytea na mesma entidade, abrir o catálogo
 * carregaria megabytes por linha para montar uma tabela que só mostra nome e
 * versão. Aqui o conteúdo só é lido por quem pede o download.
 *
 * <p>
 * A chave é o id do arquivo, não uma sequência própria — a linha não existe sem
 * ele, e a FK com {@code ON DELETE CASCADE} faz o conteúdo sumir junto.
 */
@Entity
@Table(name = "firmware_file_contents")
@NoArgsConstructor
@Getter
@Setter
public class FirmwareFileContent {

    @Id
    @Column(name = "firmware_file_id")
    private Long firmwareFileId;

    @Column(nullable = false)
    private byte[] content;

    public FirmwareFileContent(Long firmwareFileId, byte[] content) {
        this.firmwareFileId = firmwareFileId;
        this.content = content;
    }
}
