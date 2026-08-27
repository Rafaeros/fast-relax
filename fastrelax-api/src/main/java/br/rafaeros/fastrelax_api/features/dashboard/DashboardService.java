package br.rafaeros.fastrelax_api.features.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.core.tenancy.TenantSpecifications;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSessionRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkSchedule;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkScheduleRepository;
import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;
import br.rafaeros.fastrelax_api.features.dashboard.dtos.DailyUsageDTO;
import br.rafaeros.fastrelax_api.features.dashboard.dtos.DashboardResponseDTO;
import br.rafaeros.fastrelax_api.features.dashboard.dtos.DepartmentUsageDTO;
import lombok.RequiredArgsConstructor;

/**
 * Agregações do painel do RH.
 *
 * <p>
 * Existe para o painel não precisar paginar todas as sessões e contar no
 * cliente — o que ficaria lento e erraria com paginação.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MAX_RANGE_DAYS = 366;

    private final CollaboratorSessionRepository sessionRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorWorkScheduleRepository scheduleRepository;
    private final CurrentTenant currentTenant;

    /**
     * @param from início do período; ausente assume os últimos 30 dias
     * @param to   fim do período; ausente assume hoje
     */
    @Transactional(readOnly = true)
    public DashboardResponseDTO summary(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(30);
        if (end.isBefore(start)) {
            throw new BusinessException("A data final não pode ser anterior à inicial");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BusinessException("O período máximo de consulta é de " + MAX_RANGE_DAYS + " dias");
        }

        List<CollaboratorSession> sessions = sessionRepository.findByCompanyIdAndSessionDateBetween(currentTenant.companyId(), start, end);

        Map<SessionStatus, Long> byStatus = new EnumMap<>(SessionStatus.class);
        for (CollaboratorSession session : sessions) {
            byStatus.merge(session.getStatus(), 1L, (current, increment) -> current + increment);
        }

        long done = byStatus.getOrDefault(SessionStatus.DONE, 0L);
        long expired = byStatus.getOrDefault(SessionStatus.EXPIRED, 0L);
        long cancelled = byStatus.getOrDefault(SessionStatus.CANCELLED, 0L);

        // Só sessões encerradas entram na taxa: as ainda ativas não representam
        // comparecimento nem falta, e inflariam o denominador.
        long closed = done + expired + cancelled;
        Double attendanceRate = closed == 0 ? null : Math.round(done * 10000.0 / closed) / 100.0;

        return new DashboardResponseDTO(
                start,
                end,
                sessions.size(),
                byStatus.getOrDefault(SessionStatus.SCHEDULED, 0L),
                byStatus.getOrDefault(SessionStatus.STARTED, 0L),
                done,
                expired,
                cancelled,
                attendanceRate,
                countActiveCollaborators(),
                countCollaboratorsWithoutSchedule(),
                aggregateByDepartment(sessions),
                aggregateByDay(sessions, start, end));
    }

    private long countActiveCollaborators() {
        return collaboratorRepository.findAllScoped(null).stream()
                .filter(collaborator -> collaborator.isActive())
                .count();
    }

    /** Colaborador sem horário permitido configurado nunca consegue agendar — o RH precisa ver isso. */
    private long countCollaboratorsWithoutSchedule() {
        List<Long> withSchedule = scheduleRepository.findAll(
                TenantSpecifications.<CollaboratorWorkSchedule>currentCompany("collaborator", "company")).stream()
                .filter(schedule -> schedule.isActive())
                .map(schedule -> schedule.getCollaborator() != null ? schedule.getCollaborator().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .toList();

        return collaboratorRepository.findAllScoped(null).stream()
                .filter(collaborator -> collaborator.isActive())
                .filter(collaborator -> !withSchedule.contains(collaborator.getId()))
                .count();
    }

    private List<DepartmentUsageDTO> aggregateByDepartment(List<CollaboratorSession> sessions) {
        Map<Long, DepartmentAccumulator> accumulators = new LinkedHashMap<>();

        for (CollaboratorSession session : sessions) {
            Collaborator collaborator = session.getCollaborator();
            if (collaborator == null || collaborator.getDepartment() == null) {
                continue;
            }
            Long departmentId = collaborator.getDepartment().getId();
            DepartmentAccumulator accumulator = accumulators.computeIfAbsent(departmentId,
                    key -> new DepartmentAccumulator(departmentId, collaborator.getDepartment().getName()));
            accumulator.add(session.getStatus());
        }

        List<DepartmentUsageDTO> result = new ArrayList<>();
        accumulators.values().forEach(accumulator -> result.add(accumulator.toDto()));
        // Lambda no lugar de method reference: este não carrega anotação de
        // nulidade, e o compilador reclama da conversão do parâmetro implícito.
        result.sort(Comparator.comparingLong((DepartmentUsageDTO usage) -> usage.totalSessions())
                .reversed());
        return result;
    }

    /** Todos os dias do período entram, inclusive os zerados, para o gráfico não ter buracos. */
    private List<DailyUsageDTO> aggregateByDay(List<CollaboratorSession> sessions, LocalDate start, LocalDate end) {
        Map<LocalDate, DailyAccumulator> accumulators = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            accumulators.put(date, new DailyAccumulator(date));
        }
        for (CollaboratorSession session : sessions) {
            DailyAccumulator accumulator = accumulators.get(session.getSessionDate());
            if (accumulator != null) {
                accumulator.add(session.getStatus());
            }
        }
        return accumulators.values().stream().map(accumulator -> accumulator.toDto()).toList();
    }

    private static final class DepartmentAccumulator {
        private final Long id;
        private final String name;
        private long total;
        private long done;
        private long expired;

        DepartmentAccumulator(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        void add(SessionStatus status) {
            total++;
            if (status == SessionStatus.DONE) {
                done++;
            } else if (status == SessionStatus.EXPIRED) {
                expired++;
            }
        }

        DepartmentUsageDTO toDto() {
            return new DepartmentUsageDTO(id, name, total, done, expired);
        }
    }

    private static final class DailyAccumulator {
        private final LocalDate date;
        private long total;
        private long done;
        private long expired;

        DailyAccumulator(LocalDate date) {
            this.date = date;
        }

        void add(SessionStatus status) {
            total++;
            if (status == SessionStatus.DONE) {
                done++;
            } else if (status == SessionStatus.EXPIRED) {
                expired++;
            }
        }

        DailyUsageDTO toDto() {
            return new DailyUsageDTO(date, total, done, expired);
        }
    }
}
