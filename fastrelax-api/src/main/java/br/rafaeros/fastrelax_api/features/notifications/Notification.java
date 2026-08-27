package br.rafaeros.fastrelax_api.features.notifications;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedEntity;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aviso destinado a um colaborador.
 *
 * <p>
 * Guardado independentemente da entrega: o push pode não chegar (aparelho
 * desligado, permissão revogada, navegador fechado) e mesmo assim o aviso
 * precisa aparecer quando a pessoa abrir o app.
 */
@Entity
@Table(name = "notifications")
@NoArgsConstructor
@Getter
@Setter
public class Notification extends CompanyScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Carga livre para o clique abrir a tela certa, ex.: {@code {"sessionId": 42}}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isRead() {
        return readAt != null;
    }
}
