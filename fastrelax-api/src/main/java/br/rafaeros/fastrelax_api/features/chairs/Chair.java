package br.rafaeros.fastrelax_api.features.chairs;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cadeira de massagem controlada por um ESP32.
 *
 * <p>
 * A identidade é o {@link #macAddress}: fixo no hardware e único por dispositivo.
 * O {@link #ipAddress} existe só para alcançar o ESP32 e é reescrito a cada
 * heartbeat, porque DHCP troca o endereço sem aviso.
 */
@Entity
@Table(name = "chairs")
@SQLRestriction("deleted_at IS NULL")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Chair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @ColumnDefault("80")
    @Column(nullable = false)
    private int port = 80;

    /** Momento do último heartbeat; é o que define se a cadeira está online. */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @ColumnDefault("true")
    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Online enquanto o último heartbeat estiver dentro da janela tolerada. Sem
     * IP conhecido não há como comandar, então também conta como offline.
     */
    public boolean isOnline(int offlineAfterSeconds) {
        return active
                && ipAddress != null
                && lastSeenAt != null
                && lastSeenAt.isAfter(LocalDateTime.now().minusSeconds(offlineAfterSeconds));
    }

    /** URL base do ESP32, montada a partir do último endereço informado. */
    public String baseUrl() {
        return "http://" + ipAddress + ":" + port;
    }
}
