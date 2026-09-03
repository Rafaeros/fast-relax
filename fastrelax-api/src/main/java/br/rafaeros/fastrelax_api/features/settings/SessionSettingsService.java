package br.rafaeros.fastrelax_api.features.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.features.settings.dtos.SessionSettingsResponseDTO;
import br.rafaeros.fastrelax_api.features.settings.dtos.UpdateSessionSettingsRequestDTO;
import lombok.RequiredArgsConstructor;

/**
 * Configuração de agendamento da empresa em curso.
 *
 * <p>
 * As regras de sessão consultam item por item ({@code getDefaultDurationMinutes}
 * e afins) em vez de receber a entidade: assim nenhum chamador precisa saber que
 * existe uma linha por empresa, nem lembrar de criá-la.
 */
@Service
@RequiredArgsConstructor
public class SessionSettingsService {

    private final SessionSettingsRepository settingsRepository;
    private final CurrentTenant currentTenant;

    @Transactional
    public SessionSettingsResponseDTO get() {
        return new SessionSettingsResponseDTO(getOrCreate());
    }

    @Transactional
    public SessionSettingsResponseDTO update(UpdateSessionSettingsRequestDTO dto) {
        CompanySessionSettings settings = getOrCreate();
        settings.setDefaultDurationMinutes(dto.defaultDurationMinutes());
        settings.setStartGraceMinutes(dto.startGraceMinutes());
        settings.setMaxAdvanceDays(dto.maxAdvanceDays());
        settings.setStabilizationMinutes(dto.stabilizationMinutes());
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

    /** Teto de dias à frente para consultar horários e agendar. */
    @Transactional
    public int getMaxAdvanceDays() {
        return getOrCreate().getMaxAdvanceDays();
    }

    /** Minutos mínimos entre o fim de uma sessão e o início da próxima na mesma cadeira. */
    @Transactional
    public int getStabilizationMinutes() {
        return getOrCreate().getStabilizationMinutes();
    }

    /**
     * A criação da empresa já insere a linha; recriar aqui evita que uma base
     * restaurada sem ela derrube todo agendamento daquele cliente. Sem
     * {@code @Transactional} próprio: em método privado o proxy do Spring não
     * intercepta, então quem chama é que abre a transação.
     */
    private CompanySessionSettings getOrCreate() {
        Long companyId = currentTenant.companyId();
        return settingsRepository.findById(companyId)
                .orElseGet(() -> settingsRepository.save(
                        new CompanySessionSettings(currentTenant.reference())));
    }
}
