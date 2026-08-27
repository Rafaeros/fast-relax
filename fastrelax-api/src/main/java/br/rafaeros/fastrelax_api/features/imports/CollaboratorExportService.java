package br.rafaeros.fastrelax_api.features.imports;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.tenancy.TenantSpecifications;
import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkSchedule;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkScheduleRepository;
import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;
import lombok.RequiredArgsConstructor;

/**
 * Exporta os colaboradores no mesmo layout aceito pela importação, para o RH
 * baixar, editar em massa e reenviar sem remontar o arquivo.
 */
@Service
@RequiredArgsConstructor
public class CollaboratorExportService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * A importação grava o mesmo horário de segunda a sexta, então basta uma
     * referência para representar a semana. Segunda é a primeira escolha; se o
     * colaborador não tiver esse dia, cai para qualquer outro configurado.
     */
    private static final WorkDay REFERENCE_DAY = WorkDay.MONDAY;

    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorWorkScheduleRepository scheduleRepository;
    private final CryptoService cryptoService;

    /**
     * @param onlyActive true exporta só os colaboradores ativos
     * @return planilha .xlsx; sempre com o cabeçalho, mesmo sem nenhuma linha
     */
    @Transactional(readOnly = true)
    public byte[] export(boolean onlyActive) {
        // Escopado: a planilha carrega CPF de todo mundo, e sem o filtro de empresa
        // um cliente exportaria o quadro de pessoal dos outros.
        List<Collaborator> collaborators = collaboratorRepository.findAllScoped(null).stream()
                .filter(collaborator -> !onlyActive || collaborator.isActive())
                .sorted(Comparator.comparing((Collaborator collaborator) -> collaborator.getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        // Uma consulta para todos os horários, em vez de uma por colaborador.
        Map<Long, List<CollaboratorWorkSchedule>> schedulesByCollaborator = new HashMap<>();
        for (CollaboratorWorkSchedule schedule : scheduleRepository.findAll(
                TenantSpecifications.<CollaboratorWorkSchedule>currentCompany("collaborator", "company"))) {
            if (schedule.getCollaborator() != null) {
                schedulesByCollaborator
                        .computeIfAbsent(schedule.getCollaborator().getId(), key -> new ArrayList<>())
                        .add(schedule);
            }
        }

        List<String[]> rows = new ArrayList<>();
        for (Collaborator collaborator : collaborators) {
            CollaboratorWorkSchedule reference = pickReferenceSchedule(
                    schedulesByCollaborator.get(collaborator.getId()));

            rows.add(new String[] {
                    collaborator.getName(),
                    cryptoService.decrypt(collaborator.getCpfEncrypted()),
                    collaborator.getPhoneNumber(),
                    collaborator.getDepartment() != null ? collaborator.getDepartment().getName() : "",
                    reference != null ? reference.getAllowedStartTime().format(TIME_FORMAT) : "",
                    reference != null ? reference.getAllowedEndTime().format(TIME_FORMAT) : "",
                    collaborator.getEmail() != null ? collaborator.getEmail() : ""
            });
        }

        return CollaboratorSheetBuilder.build(rows);
    }

    /** Modelo em branco, com uma linha de exemplo para orientar o preenchimento. */
    public byte[] template() {
        // Tipo explícito: List.of com um array cairia no varargs e viraria List<String>.
        return CollaboratorSheetBuilder.build(List.<String[]>of(CollaboratorSheetBuilder.SAMPLE));
    }

    private CollaboratorWorkSchedule pickReferenceSchedule(List<CollaboratorWorkSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        return schedules.stream()
                .filter(schedule -> schedule.isActive())
                .min(Comparator.comparing(schedule -> schedule.getDayOfWeek() == REFERENCE_DAY ? 0 : 1))
                .orElse(null);
    }
}
