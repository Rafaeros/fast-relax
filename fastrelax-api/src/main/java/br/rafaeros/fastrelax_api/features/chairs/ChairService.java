package br.rafaeros.fastrelax_api.features.chairs;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.DeviceUnauthorizedException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairFilterDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairHeartbeatRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.CreateChairRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.RenameChairRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.SaveChairRequestDTO;
import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.companies.CompanyRepository;
import br.rafaeros.fastrelax_api.features.firmwares.FirmwareRepository;
import lombok.RequiredArgsConstructor;

/** Cadastro das cadeiras da empresa e recebimento dos heartbeats. */
@Service
@RequiredArgsConstructor
public class ChairService {

    private final ChairRepository chairRepository;
    private final CompanyRepository companyRepository;
    private final FirmwareRepository firmwareRepository;
    private final CurrentTenant currentTenant;
    private final ChairClient chairClient;
    private final ApplicationEventPublisher eventPublisher;
    private final CryptoService cryptoService;

    @Value("${app.chair.offline-after-seconds:180}")
    private int offlineAfterSeconds;

    /**
     * Precisa bater com CHAIR_COOLDOWN_MS no config.h do firmware. Se divergirem,
     * o ESP32 continua sendo a barreira real — este valor só decide se o backend
     * recusa cedo ou gasta a viagem até o dispositivo para ouvir o mesmo não.
     */
    @Value("${app.chair.cooldown-seconds:40}")
    private int cooldownSeconds;

    public Page<ChairResponseDTO> findAll(ChairFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        Specification<Chair> spec = Specification.allOf(
                ChairSpecifications.nameContains(dto != null ? dto.name() : null),
                ChairSpecifications.hasActive(dto != null ? dto.active() : null),
                ChairSpecifications.isOnline(dto != null ? dto.online() : null, offlineAfterSeconds));

        return chairRepository.findAllScoped(spec, Objects.requireNonNull(pageable))
                .map(this::toResponse);
    }

    public ChairResponseDTO findById(Long id) {
        return toResponse(findEntityById(id));
    }

    /**
     * Cadastro pela equipe da plataforma, com a empresa dona explícita. Reativa a
     * linha quando o MAC já existiu, tratando como um cadastro novo: todo campo
     * enviado é reaplicado (inclusive empresa), e o que era específico do ciclo
     * de vida anterior — pareamento de token, rede, MQTT — é zerado. A busca é
     * global porque o MAC é único no sistema inteiro; sem problema revelar isso
     * aqui porque só a equipe da plataforma chama este endpoint
     * ({@code @access.isPlatformTeam()} no controller), que já enxerga todas as
     * empresas.
     */
    @Transactional
    public ChairResponseDTO create(CreateChairRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
        String macAddress = normalizeMac(dto.macAddress());
        Chair existing = chairRepository.findByMacAddressIncludingDeleted(macAddress).orElse(null);

        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new BusinessException("Já existe uma cadeira cadastrada com este MAC address");
            }
            // Pareamento, rede e MQTT eram do ciclo de vida anterior — não vale
            // presumir que ainda batem, mesmo recadastrando na mesma empresa: o
            // ESP32 físico pode ter sido resetado nesse meio-tempo (é exatamente
            // o que causa "cadeira pareada com outro token no backend" logo após
            // um recadastro). Confiança no primeiro contato de novo, como se a
            // linha nunca tivesse existido.
            existing.setDeviceTokenEncrypted(null);
            existing.setNetworkSyncedAt(null);
            existing.setReportedSsid(null);
            existing.setMqttSyncedAt(null);
            existing.setCompany(company);
            existing.restore();
            applyFields(existing, dto.name(), macAddress, dto.ipAddress(), dto.port(), dto.firmwareId(),
                    dto.wifiBssid(), dto.mqttHost(), dto.mqttPort(), dto.mqttUsername(), dto.mqttPassword());
            return toResponse(chairRepository.save(existing));
        }

        Chair chair = new Chair();
        chair.setCompany(company);
        applyFields(chair, dto.name(), macAddress, dto.ipAddress(), dto.port(), dto.firmwareId(), dto.wifiBssid(),
                dto.mqttHost(), dto.mqttPort(), dto.mqttUsername(), dto.mqttPassword());
        return toResponse(chairRepository.save(chair));
    }

    /**
     * Edição completa, exclusiva da equipe da plataforma — inclui reatribuir a
     * empresa dona, para quando o equipamento muda de cliente fisicamente.
     */
    @Transactional
    public ChairResponseDTO update(Long id, SaveChairRequestDTO dto) {
        Chair chair = findEntityById(id);
        String macAddress = normalizeMac(dto.macAddress());

        chairRepository.findByMacAddressIncludingDeleted(macAddress)
                .filter(other -> !other.getId().equals(chair.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Já existe uma cadeira cadastrada com este MAC address");
                });

        if (!Objects.equals(chair.companyId(), dto.companyId())) {
            Company company = companyRepository.findById(dto.companyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
            chair.setCompany(company);
            // A rede sincronizada era da empresa antiga — sob outro Wi-Fi, o
            // status "aplicada"/"enviada" passaria a mentir sobre uma
            // configuração que não tem mais nada a ver com onde a cadeira está.
            chair.setNetworkSyncedAt(null);
            chair.setReportedSsid(null);
        }

        applyFields(chair, dto.name(), macAddress, dto.ipAddress(), dto.port(), dto.firmwareId(), dto.wifiBssid(),
                dto.mqttHost(), dto.mqttPort(), dto.mqttUsername(), dto.mqttPassword());
        return toResponse(chairRepository.save(chair));
    }

    /**
     * Edição pelo RH/gestor da empresa: só o nome. MAC, IP, porta, firmware e
     * BSSID continuam com quem instala o equipamento.
     */
    @Transactional
    public ChairResponseDTO rename(Long id, RenameChairRequestDTO dto) {
        Chair chair = findEntityById(id);
        chair.setName(dto.name());
        return toResponse(chairRepository.save(chair));
    }

    /**
     * Ativa/desativa e propaga aos dois lados que precisam saber: o ESP32 (relé
     * de corte de energia do painel) e, se for uma desativação, a sessão em
     * andamento naquela cadeira.
     */
    @Transactional
    public ChairResponseDTO toggleActive(Long id) {
        Chair chair = findEntityById(id);
        chair.setActive(!chair.isActive());
        Chair saved = chairRepository.save(chair);

        // Best-effort: offline agora, o próximo heartbeat reconcilia sozinho
        // (o ESP32 lê o mesmo `active` na resposta). O toggle no banco já vale
        // de qualquer forma — não é o dispositivo que decide se foi aceito.
        chairClient.pushPower(saved, saved.isActive());

        eventPublisher.publishEvent(new ChairActivationChangedEvent(saved.getId(), saved.isActive()));

        return toResponse(saved);
    }

    @Transactional
    public void softDelete(Long id) {
        Chair chair = findEntityById(id);
        chair.markDeleted();
        chairRepository.save(chair);
    }

    /**
     * Batida do ESP32: atualiza o endereço e marca presença.
     *
     * <p>
     * Chega sem tenant no contexto — o dispositivo não faz login, só apresenta o
     * token e o próprio MAC. É a cadeira encontrada que diz de qual empresa é, e
     * por isso a busca aqui é global de propósito.
     *
     * <p>
     * Só reconhece MAC já cadastrado: um dispositivo desconhecido na rede não se
     * auto-registra como cadeira, nem escolhe a empresa em que entra.
     */
    @Transactional
    public ChairResponseDTO registerHeartbeat(ChairHeartbeatRequestDTO dto, String deviceToken) {
        String macAddress = normalizeMac(dto.macAddress());
        Chair chair = chairRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cadeira não cadastrada para o MAC " + macAddress));

        authenticateDevice(chair, deviceToken);

        chair.setIpAddress(dto.ipAddress());
        if (dto.port() != null) {
            chair.setPort(dto.port());
        }
        chair.setLastSeenAt(java.time.LocalDateTime.now());
        // Guarda o SSID relatado: é o que separa "a configuração foi enviada" de
        // "a cadeira está de fato na rede certa".
        if (dto.ssid() != null && !dto.ssid().isBlank()) {
            chair.setReportedSsid(dto.ssid());
        }
        applyPhase(chair, dto);
        return toResponse(chairRepository.save(chair));
    }

    /**
     * Confiança no primeiro contato: a cadeira gera o próprio token no boot
     * (NVS) e o backend grava o que receber na primeira vez que a vir. Dali em
     * diante, exige que bata — sem isso, bastaria saber o MAC (impresso na
     * etiqueta do ESP32) para uma máquina qualquer da rede assumir a
     * identidade da cadeira.
     */
    private void authenticateDevice(Chair chair, String providedToken) {
        if (providedToken == null || providedToken.isBlank()) {
            throw new DeviceUnauthorizedException("Token do dispositivo ausente");
        }
        if (chair.getDeviceTokenEncrypted() == null) {
            chair.setDeviceTokenEncrypted(cryptoService.encrypt(providedToken));
            return;
        }
        if (!providedToken.equals(cryptoService.decrypt(chair.getDeviceTokenEncrypted()))) {
            throw new DeviceUnauthorizedException("Token do dispositivo não confere");
        }
    }

    /**
     * Sincroniza a estabilização com o que o firmware está de fato contando.
     *
     * <p>
     * O backend estima a janela ao mandar desligar, mas quem conta é o ESP32 —
     * um reset do dispositivo, por exemplo, zera a contagem dele e deixa a
     * estimativa daqui sobrando. O heartbeat é a correção: fase diferente de
     * {@code cooldown} significa cadeira livre.
     *
     * <p>
     * Firmware antigo não manda a fase; nesse caso nada muda, e a janela
     * estimada segue valendo até expirar sozinha.
     */
    private void applyPhase(Chair chair, ChairHeartbeatRequestDTO dto) {
        chair.applyPhase(dto.phase(), dto.remainingSeconds(), cooldownSeconds);
    }

    /**
     * Registra a janela de estabilização de uma cadeira.
     *
     * <p>
     * Chamado depois do desligamento (com a estimativa configurada) e quando o
     * próprio ESP32 recusa um comando informando quanto falta.
     */
    @Transactional
    public void markCoolingDown(Chair chair, long seconds) {
        chair.applyCooldown(seconds);
        chairRepository.save(chair);
    }

    /** Duração da estabilização acordada com o firmware, em segundos. */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /** Entidade crua, para quem precisa comandar o dispositivo. */
    public Chair findEntity(Long id) {
        return findEntityById(id);
    }

    /**
     * Capacidade de atendimento simultâneo da empresa.
     *
     * <p>
     * Conta as cadeiras <em>ativas</em>, não as online: o agendamento é para o
     * futuro, e uma cadeira desligada agora estará ligada amanhã ao meio-dia.
     * Usar presença aqui faria a grade encolher toda vez que a rede oscilasse.
     */
    public int countActiveChairs() {
        return chairRepository.findByCompanyIdAndActiveTrue(currentTenant.companyId()).size();
    }

    public List<Chair> listActiveChairs() {
        return chairRepository.findByCompanyIdAndActiveTrue(currentTenant.companyId());
    }

    /**
     * Cadeira livre para atender uma sessão, entre as da empresa.
     *
     * <p>
     * Agora que uma empresa pode ter várias, a escolha é a primeira online que
     * não esteja se estabilizando — e é este o ponto a mudar quando a alocação
     * precisar de outro critério (a menos usada do dia, por exemplo).
     */
    public Chair findAvailableChair() {
        List<Chair> online = onlineChairs();

        if (online.isEmpty()) {
            throw new BusinessException("Nenhuma cadeira disponível no momento. Procure o RH.");
        }

        return online.stream()
                .filter(chair -> !chair.isCoolingDown())
                .findFirst()
                // Todas estabilizando: recusa aqui, sem gastar a viagem até o
                // dispositivo, e diz quanto falta na que libera primeiro.
                .orElseThrow(() -> new BusinessException(
                        "A cadeira está se estabilizando após a sessão anterior. Tente novamente em "
                                + online.stream()
                                        .mapToLong(Chair::cooldownSecondsRemaining)
                                        .min()
                                        .orElse(cooldownSeconds)
                                + " segundos."));
    }

    private List<Chair> onlineChairs() {
        return chairRepository.findByCompanyIdAndActiveTrue(currentTenant.companyId()).stream()
                .filter(chair -> chair.isOnline(offlineAfterSeconds))
                .toList();
    }

    private void applyFields(Chair chair, String name, String macAddress, String ipAddress, Integer port,
            Long firmwareId, String wifiBssid, String mqttHost, Integer mqttPort, String mqttUsername,
            String mqttPassword) {
        chair.setName(name);
        chair.setMacAddress(macAddress);
        if (ipAddress != null && !ipAddress.isBlank()) {
            chair.setIpAddress(ipAddress.trim());
        }
        if (port != null) {
            chair.setPort(port);
        }
        if (firmwareId != null) {
            chair.setFirmware(firmwareRepository.findById(firmwareId)
                    .orElseThrow(() -> new ResourceNotFoundException("Firmware não encontrado")));
        }
        // Em branco apaga a fixação: é assim que se volta a deixar o ESP32
        // escolher o AP sozinho, sem precisar de outro campo para isso.
        String bssid = wifiBssid == null ? "" : wifiBssid.trim().toUpperCase().replace('-', ':');
        chair.setWifiBssid(bssid.isEmpty() ? null : bssid);

        applyMqttOverride(chair, mqttHost, mqttPort, mqttUsername, mqttPassword);
    }

    /**
     * Broker MQTT específico desta cadeira, mesmo critério do wifiBssid: em
     * branco apaga o override e volta a valer o padrão global.
     *
     * <p>
     * A senha segue regra própria: em branco <em>mantém</em> a já gravada, ao
     * contrário do host/porta/usuário. Reenviar o formulário de edição sem
     * digitar a senha de novo não deveria apagá-la — só o host em branco limpa
     * tudo, host preenchido nunca apaga a senha sozinho.
     */
    private void applyMqttOverride(Chair chair, String mqttHost, Integer mqttPort, String mqttUsername,
            String mqttPassword) {
        String host = blankToNull(mqttHost);

        if (host == null) {
            if (chair.hasMqttOverride()) {
                // Não se sabe mais qual broker o dispositivo tem gravado: a
                // config enviada era a de um override que acabou de sumir.
                chair.setMqttSyncedAt(null);
            }
            chair.setMqttHost(null);
            chair.setMqttPort(null);
            chair.setMqttUsername(null);
            chair.setMqttPasswordEncrypted(null);
            return;
        }

        String username = blankToNull(mqttUsername);
        boolean changed = !host.equals(chair.getMqttHost())
                || !Objects.equals(mqttPort, chair.getMqttPort())
                || !Objects.equals(username, chair.getMqttUsername());

        chair.setMqttHost(host);
        chair.setMqttPort(mqttPort);
        chair.setMqttUsername(username);

        if (mqttPassword != null && !mqttPassword.isBlank()) {
            chair.setMqttPasswordEncrypted(cryptoService.encrypt(mqttPassword));
            changed = true;
        }

        if (changed) {
            chair.setMqttSyncedAt(null);
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Aceita "aa-bb-cc-dd-ee-ff" e grava sempre em maiúsculas com dois-pontos. */
    private String normalizeMac(String macAddress) {
        return macAddress.trim().toUpperCase().replace('-', ':');
    }

    /** Escopada: um id de outra empresa responde 404, como se não existisse. */
    private Chair findEntityById(Long id) {
        return chairRepository.findByIdScoped(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Cadeira não encontrada"));
    }

    private ChairResponseDTO toResponse(Chair chair) {
        return new ChairResponseDTO(chair, offlineAfterSeconds);
    }
}
