package br.rafaeros.fastrelax_api.features.chairs.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.chairs.Chair;

public record ChairResponseDTO(
    Long id,
    String name,
    String macAddress,
    String ipAddress,
    int port,
    boolean active,
    /** Calculado a partir do último heartbeat, não persistido. */
    boolean online,
    LocalDateTime lastSeenAt,
    LocalDateTime createdAt
) {
    public ChairResponseDTO(Chair chair, int offlineAfterSeconds) {
        this(chair.getId(), chair.getName(), chair.getMacAddress(), chair.getIpAddress(), chair.getPort(),
                chair.isActive(), chair.isOnline(offlineAfterSeconds), chair.getLastSeenAt(),
                chair.getCreatedAt());
    }
}
