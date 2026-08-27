package br.rafaeros.fastrelax_api.features.notifications;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
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

/** Destino de push de um aparelho. Um colaborador pode ter vários. */
@Entity
@Table(name = "device_tokens")
@NoArgsConstructor
@Getter
@Setter
public class DeviceToken extends CompanyScopedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    /** Token do FCM. Preenchido em ANDROID e IOS; nulo em WEB. */
    @Column(columnDefinition = "TEXT")
    private String token;

    /**
     * Inscrição do navegador. Preenchida em WEB; nula nas demais plataformas.
     *
     * <p>
     * Web Push não entrega por token: o navegador devolve o endereço do serviço
     * de push dele mais as chaves que cifram a mensagem. São dados demais para
     * uma coluna de texto, e o JSONB mantém o formato igual ao que o navegador
     * produz.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "push_subscription", columnDefinition = "jsonb")
    private PushSubscription pushSubscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Platform {
        ANDROID,
        IOS,
        WEB
    }
}
