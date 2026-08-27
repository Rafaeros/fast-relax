package br.rafaeros.fastrelax_api.features.firmwares;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório próprio do binário.
 *
 * <p>
 * Existe para gravar o arquivo <em>diretamente</em>, em vez de deixá-lo subir
 * pelo cascade de {@link Firmware}. Pelo cascade, o {@code save} do pai vira um
 * {@code merge} — o pai já tem id —, e o merge de um filho transiente cria uma
 * <b>cópia</b> gerenciada: o id gerado fica nela, e o objeto original continua
 * com {@code null}. O conteúdo, cuja chave é justamente esse id, não tinha como
 * ser gravado.
 */
public interface FirmwareFileRepository extends JpaRepository<FirmwareFile, Long> {
}
