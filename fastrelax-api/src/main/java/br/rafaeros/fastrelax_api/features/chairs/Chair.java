package br.rafaeros.fastrelax_api.features.chairs;

import java.time.Duration;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import br.rafaeros.fastrelax_api.core.tenancy.SoftDeletableCompanyEntity;
import br.rafaeros.fastrelax_api.features.firmwares.Firmware;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cadeira de massagem controlada por um ESP32, instalada em uma empresa.
 *
 * <p>
 * A identidade é o {@link #macAddress}: fixo no hardware e único no sistema
 * inteiro, não por empresa — o mesmo equipamento não pode estar instalado em
 * dois clientes ao mesmo tempo, e é isso que a unicidade global garante. O
 * {@link #ipAddress} existe só para alcançar o ESP32 e é reescrito a cada
 * heartbeat, porque DHCP troca o endereço sem aviso.
 */
@Entity
@Table(name = "chairs")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@Getter
@Setter
public class Chair extends SoftDeletableCompanyEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    /**
     * Versão gravada no dispositivo, mantida pela Physical. Nula enquanto o
     * equipamento nunca foi atualizado pelo processo formal.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "firmware_id")
    private Firmware firmware;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @ColumnDefault("80")
    @Column(nullable = false)
    private int port = 80;

    /** Momento do último heartbeat; é o que define se a cadeira está online. */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    /**
     * Até quando a cadeira está se estabilizando após uma sessão.
     *
     * <p>
     * O relé aperta o botão do aparelho em vez de cortar a alimentação, e depois
     * do pulso que desliga a cadeira leva alguns segundos recolhendo os
     * mecanismos — apertar o botão nesse meio tempo a trava. Quem impõe a
     * barreira é o ESP32, que recusa o comando com 409; aqui fica o que o backend
     * sabe da janela, para escolher outra cadeira ou recusar cedo sem gastar a
     * viagem até o dispositivo.
     *
     * <p>
     * Nulo ou no passado significa livre.
     */
    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    /**
     * Ponto de acesso em que esta cadeira deve entrar, dentro do SSID da
     * empresa. Nulo deixa o ESP32 escolher o de melhor sinal.
     *
     * <p>
     * Por cadeira, e não por empresa, porque cada uma está fisicamente perto de
     * um AP diferente — um BSSID único empurraria todas para o mesmo ponto,
     * inclusive as do outro galpão.
     */
    @Column(name = "wifi_bssid", length = 17)
    private String wifiBssid;

    /**
     * Quando o ESP32 confirmou que gravou a configuração na NVS.
     *
     * <p>
     * Separa "o SYSADMIN preencheu o formulário" de "a cadeira está com a rede
     * certa". A diferença entre os dois é uma cadeira que não volta depois que
     * o Wi-Fi da empresa muda.
     */
    @Column(name = "network_synced_at")
    private LocalDateTime networkSyncedAt;

    /** SSID relatado no último heartbeat — a confirmação de que foi aplicado. */
    @Column(name = "reported_ssid", length = 64)
    private String reportedSsid;

    /**
     * Broker MQTT específico desta cadeira. Nulo/vazio usa o padrão global
     * ({@code app.mqtt.device-*}), que é o caso comum — o mesmo broker que o
     * firmware já traz embutido em config.h. Existe por cadeira, e não por
     * empresa, pelo mesmo motivo do {@link #wifiBssid}: um override raro que
     * não deveria arrastar o parque inteiro junto.
     */
    @Column(name = "mqtt_host", length = 255)
    private String mqttHost;

    @Column(name = "mqtt_port")
    private Integer mqttPort;

    @Column(name = "mqtt_username", length = 100)
    private String mqttUsername;

    /**
     * Cifrada (AES-GCM, mesmo {@code CryptoService} do CPF, da senha de Wi-Fi
     * e do token de dispositivo). Nunca aparece em claro em DTO nenhum.
     */
    @Column(name = "mqtt_password_encrypted", columnDefinition = "TEXT")
    private String mqttPasswordEncrypted;

    /** Quando o ESP32 confirmou ter gravado a configuração de MQTT na NVS. */
    @Column(name = "mqtt_synced_at")
    private LocalDateTime mqttSyncedAt;

    /**
     * Segredo desta cadeira, cifrado (AES-GCM, mesmo {@code CryptoService} do
     * CPF e da senha de Wi-Fi).
     *
     * <p>
     * Gerado pelo próprio ESP32 no primeiro boot, não pela plataforma — nulo
     * até o primeiro heartbeat, que é quando o backend o vê e grava
     * (confiança no primeiro contato). Substitui o antigo segredo único
     * compartilhado entre todas as cadeiras: dali em diante, um MAC com token
     * divergente é recusado.
     */
    @Column(name = "device_token_encrypted", columnDefinition = "TEXT")
    private String deviceTokenEncrypted;

    /**
     * A cadeira está na rede que a empresa configurou.
     *
     * <p>
     * Compara o que o dispositivo relatou com o que deveria estar rodando. Sem
     * isso, uma cadeira que recebeu o SSID novo mas não conseguiu entrar nele
     * apareceria como configurada — e continuaria no ar pela rede antiga até
     * alguém desligar o AP velho.
     */
    public boolean isOnConfiguredNetwork() {
        if (getCompany() == null || !getCompany().hasWifi()) {
            return false;
        }
        return getCompany().getWifiSsid().equals(reportedSsid);
    }

    /** Tem broker próprio configurado; sem isto, vale o padrão global. */
    public boolean hasMqttOverride() {
        return mqttHost != null && !mqttHost.isBlank();
    }

    /**
     * Online enquanto o último heartbeat estiver dentro da janela tolerada. Sem
     * IP conhecido não há como comandar, então também conta como offline.
     */
    public boolean isOnline(int offlineAfterSeconds) {
        return isActive()
                && ipAddress != null
                && lastSeenAt != null
                && lastSeenAt.isAfter(LocalDateTime.now().minusSeconds(offlineAfterSeconds));
    }

    /** URL base do ESP32, montada a partir do último endereço informado. */
    public String baseUrl() {
        return "http://" + ipAddress + ":" + port;
    }

    public boolean isCoolingDown() {
        return cooldownUntil != null && cooldownUntil.isAfter(LocalDateTime.now());
    }

    /** Quanto falta da estabilização, arredondado para cima; 0 quando já passou. */
    public long cooldownSecondsRemaining() {
        if (!isCoolingDown()) {
            return 0;
        }
        // Arredonda para cima: dizer "faltam 0s" com a janela ainda aberta faria
        // o colaborador tentar de novo e receber a mesma recusa.
        return Duration.between(LocalDateTime.now(), cooldownUntil).plusSeconds(1).toSeconds();
    }

    /**
     * Substitui a janela conhecida. {@code seconds <= 0} marca a cadeira como
     * livre.
     *
     * <p>
     * Sobrescreve em vez de estender porque a fonte mais recente é sempre a mais
     * confiável: o heartbeat traz o que o firmware está de fato contando, e ele
     * manda mais que a estimativa feita aqui no momento do desligamento.
     */
    public void applyCooldown(long seconds) {
        cooldownUntil = seconds > 0 ? LocalDateTime.now().plusSeconds(seconds) : null;
    }

    /**
     * Aplica a fase que o firmware relatou — heartbeat HTTP ou status MQTT, as
     * duas fontes usam o mesmo vocabulário ({@code idle}, {@code cooldown}
     * etc.), porque as duas leem o mesmo {@code session::Snapshot} do lado do
     * ESP32.
     *
     * <p>
     * Fica na entidade, e não duplicada nos dois serviços que a chamam, porque
     * é a mesma regra de negócio (fase diferente de cooldown = livre) nos dois
     * transportes.
     *
     * @param defaultCooldownSeconds usado quando a fase é cooldown mas o
     *                                firmware não informou quanto falta
     */
    public void applyPhase(String phase, Integer remainingSeconds, int defaultCooldownSeconds) {
        if (phase == null || phase.isBlank()) {
            return;
        }
        if (!"cooldown".equalsIgnoreCase(phase)) {
            applyCooldown(0);
            return;
        }
        applyCooldown(remainingSeconds != null ? remainingSeconds : defaultCooldownSeconds);
    }
}
