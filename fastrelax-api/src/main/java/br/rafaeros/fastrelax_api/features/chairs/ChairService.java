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

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairFilterDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairHeartbeatRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.SaveChairRequestDTO;
import br.rafaeros.fastrelax_api.features.firmwares.FirmwareRepository;
import lombok.RequiredArgsConstructor;

/** Cadastro das cadeiras da empresa e recebimento dos heartbeats. */
@Service
@RequiredArgsConstructor
public class ChairService {

    private final ChairRepository chairRepository;
    private final FirmwareRepository firmwareRepository;
    private final CurrentTenant currentTenant;
    private final ChairClient chairClient;
    private final ApplicationEventPublisher eventPublisher;

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
     * Cadastro pelo RH. Reativa a linha quando o MAC já existiu <em>na própria
     * empresa</em>: é o mesmo hardware voltando, não uma cadeira nova.
     */
    @Transactional
    public ChairResponseDTO create(SaveChairRequestDTO dto) {
        String macAddress = normalizeMac(dto.macAddress());
        Chair existing = chairRepository.findByMacAddressIncludingDeleted(macAddress).orElse(null);

        if (existing != null) {
            // A busca é global porque o MAC é único no sistema inteiro. Cadeira de
            // outro cliente responde igual a uma já cadastrada aqui: dizer "está em
            // outra empresa" revelaria o parque instalado alheio.
            if (existing.getDeletedAt() == null || !isOwnedByCurrentCompany(existing)) {
                throw new BusinessException("Já existe uma cadeira cadastrada com este MAC address");
            }
            existing.restore();
            applyFields(existing, dto, macAddress);
            return toResponse(chairRepository.save(existing));
        }

        Chair chair = new Chair();
        chair.setCompany(currentTenant.reference());
        applyFields(chair, dto, macAddress);
        return toResponse(chairRepository.save(chair));
    }

    @Transactional
    public ChairResponseDTO update(Long id, SaveChairRequestDTO dto) {
        Chair chair = findEntityById(id);
        String macAddress = normalizeMac(dto.macAddress());

        chairRepository.findByMacAddressIncludingDeleted(macAddress)
                .filter(other -> !other.getId().equals(chair.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Já existe uma cadeira cadastrada com este MAC address");
                });

        applyFields(chair, dto, macAddress);
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
     * segredo do firmware e o próprio MAC. É a cadeira encontrada que diz de qual
     * empresa é, e por isso a busca aqui é global de propósito.
     *
     * <p>
     * Só reconhece MAC já cadastrado: um dispositivo desconhecido na rede não se
     * auto-registra como cadeira, nem escolhe a empresa em que entra.
     */
    @Transactional
    public ChairResponseDTO registerHeartbeat(ChairHeartbeatRequestDTO dto) {
        String macAddress = normalizeMac(dto.macAddress());
        Chair chair = chairRepository.findByMacAddress(macAddress)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cadeira não cadastrada para o MAC " + macAddress));

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
        if (dto.phase() == null || dto.phase().isBlank()) {
            return;
        }
        if (!dto.isCoolingDown()) {
            chair.applyCooldown(0);
            return;
        }
        int remaining = dto.remainingSeconds() != null ? dto.remainingSeconds() : cooldownSeconds;
        chair.applyCooldown(remaining);
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

    private boolean isOwnedByCurrentCompany(Chair chair) {
        return Objects.equals(chair.companyId(), currentTenant.companyId());
    }

    private void applyFields(Chair chair, SaveChairRequestDTO dto, String macAddress) {
        chair.setName(dto.name());
        chair.setMacAddress(macAddress);
        if (dto.ipAddress() != null && !dto.ipAddress().isBlank()) {
            chair.setIpAddress(dto.ipAddress().trim());
        }
        if (dto.port() != null) {
            chair.setPort(dto.port());
        }
        if (dto.firmwareId() != null) {
            chair.setFirmware(firmwareRepository.findById(dto.firmwareId())
                    .orElseThrow(() -> new ResourceNotFoundException("Firmware não encontrado")));
        }
        // Em branco apaga a fixação: é assim que se volta a deixar o ESP32
        // escolher o AP sozinho, sem precisar de outro campo para isso.
        String bssid = dto.wifiBssid() == null ? "" : dto.wifiBssid().trim().toUpperCase().replace('-', ':');
        chair.setWifiBssid(bssid.isEmpty() ? null : bssid);
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
