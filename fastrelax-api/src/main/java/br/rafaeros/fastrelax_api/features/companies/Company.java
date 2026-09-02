package br.rafaeros.fastrelax_api.features.companies;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import br.rafaeros.fastrelax_api.features.locations.Address;
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
 * Empresa cliente — o tenant do sistema.
 *
 * <p>
 * Toda linha operacional (departamento, colaborador, cadeira, sessão) aponta
 * para uma empresa, e é esse vínculo que o {@code TenantContext} usa para
 * decidir o que cada requisição enxerga.
 *
 * <p>
 * O slug é a identidade pública na tela de login: é por ele que o colaborador
 * diz de qual empresa é, já que o CPF só é único dentro do tenant. O CNPJ
 * continua no cadastro como identidade fiscal, mas ninguém mais digita os 14
 * dígitos para entrar.
 */
@Entity
@Table(name = "companies")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    /** Identidade fiscal. Só dígitos: a normalização acontece antes de gravar. */
    @Column(nullable = false, unique = true, length = 20)
    private String cnpj;

    @Column(nullable = false)
    private String name;

    /**
     * Identificador curto e público usado no login do colaborador.
     *
     * <p>
     * Cadastrado explicitamente ou derivado da primeira palavra do nome (ver
     * {@link br.rafaeros.fastrelax_api.core.util.SlugUtils}). Sempre minúsculo
     * e sem acento — é o que compara igual, independente de como a pessoa
     * digitou.
     */
    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    /**
     * SSID da rede em que as cadeiras desta empresa entram.
     *
     * <p>
     * Fica na empresa, e não em cada cadeira, porque é propriedade da planta:
     * trocar a senha do Wi-Fi precisa ser uma edição só, não uma por
     * equipamento.
     */
    @Column(name = "wifi_ssid", length = 64)
    private String wifiSsid;

    /**
     * Senha do Wi-Fi em AES-GCM, como o CPF e pelo mesmo motivo: é segredo de
     * terceiro guardado por nós.
     *
     * <p>
     * Nunca sai da API em claro. Quem a decifra é o serviço que empurra a
     * configuração para o ESP32, e o destino é a NVS do dispositivo.
     */
    @Column(name = "wifi_password_encrypted", columnDefinition = "TEXT")
    private String wifiPasswordEncrypted;

    @Column(name = "wifi_updated_at")
    private LocalDateTime wifiUpdatedAt;

    /** Rede configurada o bastante para ser empurrada às cadeiras. */
    public boolean hasWifi() {
        return wifiSsid != null && !wifiSsid.isBlank() && wifiPasswordEncrypted != null;
    }

    /**
     * Empresa inativa é contrato suspenso: ninguém dela autentica, nem o gestor
     * nem os colaboradores. É diferente de removida, que some das listagens.
     */
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

    public boolean isEnabled() {
        return active && deletedAt == null;
    }
}
