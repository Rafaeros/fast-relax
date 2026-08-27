package br.rafaeros.fastrelax_api.features.chairs.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.chairs.Chair;

public record ChairResponseDTO(
    Long id,
    String name,
    String macAddress,
    String ipAddress,
    int port,
    /**
     * Empresa dona do equipamento.
     *
     * <p>
     * Existe para a listagem do SYSADMIN, que atravessa clientes: sem o nome, o
     * parque inteiro vira uma lista de cadeiras sem contexto. Para quem opera
     * dentro de uma empresa é sempre a própria, e a tela não exibe a coluna.
     */
    Long companyId,
    String companyName,
    /** Versão gravada no dispositivo; nula quando nunca passou pela atualização formal. */
    Long firmwareId,
    String firmwareVersion,
    /** Ponto de acesso fixado; nulo deixa o ESP32 escolher o de melhor sinal. */
    String wifiBssid,
    /** Quando o ESP32 confirmou ter gravado a configuração de rede. */
    LocalDateTime networkSyncedAt,
    /** SSID que o dispositivo relatou no último heartbeat. */
    String reportedSsid,
    /**
     * O dispositivo está na rede que a empresa configurou.
     *
     * <p>
     * Diferente de {@code networkSyncedAt}: aquele diz que o envio foi aceito,
     * este que a cadeira de fato entrou na rede. Uma cadeira pode ter gravado o
     * SSID novo e continuar no antigo, e é essa a que some quando o AP velho
     * for desligado.
     */
    boolean onConfiguredNetwork,
    boolean active,
    /** Calculado a partir do último heartbeat, não persistido. */
    boolean online,
    /**
     * Estabilizando após a sessão anterior: online, mas ainda recusando
     * acionamento. É um estado à parte de offline — a cadeira está viva e
     * respondendo, só não pode ser ligada ainda.
     */
    boolean coolingDown,
    /** Quanto falta da estabilização; 0 quando a cadeira já está livre. */
    long cooldownSecondsRemaining,
    LocalDateTime lastSeenAt,
    LocalDateTime createdAt
) {
    public ChairResponseDTO(Chair chair, int offlineAfterSeconds) {
        this(chair.getId(), chair.getName(), chair.getMacAddress(), chair.getIpAddress(),
                chair.getPort(),
                chair.companyId(),
                chair.getCompany() != null ? chair.getCompany().getName() : null,
                chair.getFirmware() != null ? chair.getFirmware().getId() : null,
                chair.getFirmware() != null ? chair.getFirmware().getVersion() : null,
                chair.getWifiBssid(),
                chair.getNetworkSyncedAt(),
                chair.getReportedSsid(),
                chair.isOnConfiguredNetwork(),
                chair.isActive(), chair.isOnline(offlineAfterSeconds), chair.isCoolingDown(),
                chair.cooldownSecondsRemaining(), chair.getLastSeenAt(), chair.getCreatedAt());
    }
}
