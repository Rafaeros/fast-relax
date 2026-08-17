package br.rafaeros.fastrelax_api.features.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.features.settings.dtos.SessionSettingsResponseDTO;
import br.rafaeros.fastrelax_api.features.settings.dtos.UpdateSessionSettingsRequestDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionSettingsService {

    private final SessionSettingsRepository settingsRepository;

    @Transactional
    public SessionSettingsResponseDTO get() {
        return new SessionSettingsResponseDTO(getOrCreate());
    }

    @Transactional
    public SessionSettingsResponseDTO update(UpdateSessionSettingsRequestDTO dto) {
        SessionSettings settings = getOrCreate();
        settings.setDefaultDurationMinutes(dto.defaultDurationMinutes());
        settings.setStartGraceMinutes(dto.startGraceMinutes());
        settings.setEarlyStartMinutes(dto.earlyStartMinutes());
        settings.setMaxAdvanceDays(dto.maxAdvanceDays());
        return new SessionSettingsResponseDTO(settingsRepository.save(settings));
    }

    /** Duração aplicada a novos agendamentos. */
    @Transactional
    public int getDefaultDurationMinutes() {
        return getOrCreate().getDefaultDurationMinutes();
    }

    /** Minutos de tolerância antes de a sessão agendada expirar por não ter sido iniciada. */
    @Transactional
    public int getStartGraceMinutes() {
        return getOrCreate().getStartGraceMinutes();
    }

    /** Minutos de antecedência tolerados para iniciar antes do horário agendado. */
    @Transactional
    public int getEarlyStartMinutes() {
        return getOrCreate().getEarlyStartMinutes();
    }

    /** Teto de dias à frente para consultar horários e agendar. */
    @Transactional
    public int getMaxAdvanceDays() {
        return getOrCreate().getMaxAdvanceDays();
    }

    /**
     * A migration já insere a linha; recriar aqui evita que um banco restaurado
     * sem ela derrube todo agendamento. Sem {@code @Transactional} próprio: em
     * método privado o proxy do Spring não intercepta, então quem chama é que
     * abre a transação.
     */
    private SessionSettings getOrCreate() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> settingsRepository.save(new SessionSettings()));
    }
}
