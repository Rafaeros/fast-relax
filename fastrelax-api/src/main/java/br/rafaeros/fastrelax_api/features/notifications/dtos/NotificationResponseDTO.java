package br.rafaeros.fastrelax_api.features.notifications.dtos;

import java.time.LocalDateTime;
import java.util.Map;

import br.rafaeros.fastrelax_api.features.notifications.Notification;
import br.rafaeros.fastrelax_api.features.notifications.NotificationType;

/**
 * @param type      valor do enum, estável para o cliente escolher ícone e rota
 * @param typeLabel mesmo tipo em português, pronto para exibição
 * @param read      conveniência para a lista não ter que comparar {@code readAt}
 */
public record NotificationResponseDTO(
    Long id,
    NotificationType type,
    String typeLabel,
    String title,
    String body,
    Map<String, Object> data,
    boolean read,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
    public NotificationResponseDTO(Notification entity) {
        this(
            entity.getId(),
            entity.getType(),
            entity.getType() != null ? entity.getType().getLabel() : null,
            entity.getTitle(),
            entity.getBody(),
            entity.getData(),
            entity.isRead(),
            entity.getReadAt(),
            entity.getCreatedAt()
        );
    }
}
