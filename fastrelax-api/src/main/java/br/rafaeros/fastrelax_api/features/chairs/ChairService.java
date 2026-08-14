package br.rafaeros.fastrelax_api.features.chairs;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairFilterDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairHeartbeatRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.SaveChairRequestDTO;
import lombok.RequiredArgsConstructor;

/** Cadastro das cadeiras e recebimento dos heartbeats. */
@Service
@RequiredArgsConstructor
public class ChairService {

    private final ChairRepository chairRepository;

    @Value("${app.chair.offline-after-seconds:180}")
    private int offlineAfterSeconds;

    public Page<ChairResponseDTO> findAll(ChairFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        Specification<Chair> spec = Specification.allOf(
                ChairSpecifications.nameContains(dto != null ? dto.name() : null),
                ChairSpecifications.hasActive(dto != null ? dto.active() : null),
                ChairSpecifications.isOnline(dto != null ? dto.online() : null, offlineAfterSeconds));

        return chairRepository.findAll(spec, Objects.requireNonNull(pageable)).map(this::toResponse);
    }

    public ChairResponseDTO findById(Long id) {
        return toResponse(findEntityById(id));
    }

    /**
     * Cadastro pelo RH. Reativa a linha quando o MAC já existiu: o dispositivo é o
     * mesmo hardware voltando, não uma cadeira nova.
     */
    @Transactional
    public ChairResponseDTO create(SaveChairRequestDTO dto) {
        String macAddress = normalizeMac(dto.macAddress());
        Chair existing = chairRepository.findByMacAddressIncludingDeleted(macAddress).orElse(null);

        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new BusinessException("Já existe uma cadeira cadastrada com este MAC address");
            }
            existing.setDeletedAt(null);
            existing.setActive(true);
            applyFields(existing, dto, macAddress);
            return toResponse(chairRepository.save(existing));
        }

        Chair chair = new Chair();
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

    @Transactional
    public ChairResponseDTO toggleActive(Long id) {
        Chair chair = findEntityById(id);
        chair.setActive(!chair.isActive());
        return toResponse(chairRepository.save(chair));
    }

    @Transactional
    public void softDelete(Long id) {
        Chair chair = findEntityById(id);
        chair.setActive(false);
        chair.setDeletedAt(LocalDateTime.now());
        chairRepository.save(chair);
    }

    /**
     * Batida do ESP32: atualiza o endereço e marca presença.
     *
     * <p>
     * Só reconhece MAC já cadastrado — um dispositivo desconhecido na rede não se
     * auto-registra como cadeira. O RH cadastra primeiro, o hardware se anuncia
     * depois.
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
        chair.setLastSeenAt(LocalDateTime.now());
        return toResponse(chairRepository.save(chair));
    }

    /**
     * Cadeira disponível para atender uma sessão. Hoje há uma só, então devolve a
     * primeira online; quando houver mais, este é o ponto que decide a alocação.
     */
    public Chair findAvailableChair() {
        return chairRepository.findByActiveTrue().stream()
                .filter(chair -> chair.isOnline(offlineAfterSeconds))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Nenhuma cadeira disponível no momento. Procure o RH."));
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
    }

    /** Aceita "aa-bb-cc-dd-ee-ff" e grava sempre em maiúsculas com dois-pontos. */
    private String normalizeMac(String macAddress) {
        return macAddress.trim().toUpperCase().replace('-', ':');
    }

    private Chair findEntityById(Long id) {
        return chairRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Cadeira não encontrada"));
    }

    private ChairResponseDTO toResponse(Chair chair) {
        return new ChairResponseDTO(chair, offlineAfterSeconds);
    }
}
