package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.core.tenancy.TenantSpecifications;
import br.rafaeros.fastrelax_api.core.security.AccessGuard;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorWorkScheduleDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorWorkScheduleFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorWorkScheduleResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.WeeklyScheduleRequestDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.WorkScheduleItemDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollaboratorWorkScheduleService {

    private final CollaboratorWorkScheduleRepository scheduleRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final AccessGuard access;

    public Page<CollaboratorWorkScheduleResponseDTO> findAll(CollaboratorWorkScheduleFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        // Colaborador logado só vê as próprias linhas: o id dele sobrepõe qualquer
        // collaboratorId que venha no filtro.
        Long collaboratorId = dto != null ? dto.collaboratorId() : null;
        collaboratorId = Principals.collaborator().map(Collaborator::getId).orElse(collaboratorId);

        Specification<CollaboratorWorkSchedule> spec = Specification.allOf(
                CollaboratorWorkScheduleSpecifications.hasDayOfWeek(dto != null ? dto.dayOfWeek() : null),
                CollaboratorWorkScheduleSpecifications.hasActive(dto != null ? dto.active() : null),
                CollaboratorWorkScheduleSpecifications.hasCollaborator(collaboratorId));

        return scheduleRepository.findAll(scopedToCompany(spec), Objects.requireNonNull(pageable))
                .map(schedule -> new CollaboratorWorkScheduleResponseDTO(schedule));
    }

    public CollaboratorWorkScheduleResponseDTO findById(Long id) {
        return new CollaboratorWorkScheduleResponseDTO(findEntityById(id));
    }

    /** Semana do colaborador logado, sem precisar informar o id. */
    public List<CollaboratorWorkScheduleResponseDTO> findMyWeeklySchedule() {
        return findWeeklySchedule(Principals.requireCollaborator().getId());
    }

    /** Every active weekday configured for a collaborator, ordered Monday to Friday. */
    public List<CollaboratorWorkScheduleResponseDTO> findWeeklySchedule(Long collaboratorId) {
        requireCollaboratorAccess(collaboratorId);
        return scheduleRepository.findByCollaboratorId(Objects.requireNonNull(collaboratorId)).stream()
                .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .map(schedule -> new CollaboratorWorkScheduleResponseDTO(schedule))
                .toList();
    }

    /**
     * Replaces the collaborator's whole week: days in the payload are created or
     * updated, days left out are soft-deleted. Idempotent, so the caller can send
     * the same body twice without creating duplicates.
     */
    @Transactional
    public List<CollaboratorWorkScheduleResponseDTO> replaceWeeklySchedule(Long collaboratorId,
            WeeklyScheduleRequestDTO dto) {
        Collaborator collaborator = collaboratorRepository.findByIdScoped(Objects.requireNonNull(collaboratorId))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));

        Map<WorkDay, WorkScheduleItemDTO> requested = indexByDay(dto.schedules());

        // Soft-deleted rows still occupy (collaborator_id, day_of_week), so they are
        // loaded here and reactivated rather than inserted again.
        Map<WorkDay, CollaboratorWorkSchedule> existing = new EnumMap<>(WorkDay.class);
        for (CollaboratorWorkSchedule schedule : scheduleRepository
                .findByCollaboratorIdIncludingDeleted(collaboratorId)) {
            existing.put(schedule.getDayOfWeek(), schedule);
        }

        List<CollaboratorWorkSchedule> saved = new ArrayList<>();
        for (Map.Entry<WorkDay, WorkScheduleItemDTO> entry : requested.entrySet()) {
            WorkScheduleItemDTO item = entry.getValue();
            validateAllowedWindow(item.allowedStartTime(), item.allowedEndTime());

            CollaboratorWorkSchedule schedule = existing.get(entry.getKey());
            if (schedule == null) {
                schedule = new CollaboratorWorkSchedule();
                schedule.setCollaborator(collaborator);
                schedule.setDayOfWeek(entry.getKey());
            }
            schedule.setAllowedStartTime(item.allowedStartTime());
            schedule.setAllowedEndTime(item.allowedEndTime());
            schedule.setActive(true);
            schedule.setDeletedAt(null);
            saved.add(scheduleRepository.save(schedule));
        }

        for (Map.Entry<WorkDay, CollaboratorWorkSchedule> entry : existing.entrySet()) {
            CollaboratorWorkSchedule schedule = entry.getValue();
            if (!requested.containsKey(entry.getKey()) && schedule.getDeletedAt() == null) {
                schedule.setActive(false);
                schedule.setDeletedAt(LocalDateTime.now());
                scheduleRepository.save(schedule);
            }
        }

        return saved.stream()
                .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .map(schedule -> new CollaboratorWorkScheduleResponseDTO(schedule))
                .toList();
    }

    @Transactional
    public CollaboratorWorkScheduleResponseDTO create(CollaboratorWorkScheduleDTO dto) {
        Collaborator collaborator = collaboratorRepository.findByIdScoped(Objects.requireNonNull(dto.collaboratorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
        validateAllowedWindow(dto.allowedStartTime(), dto.allowedEndTime());

        CollaboratorWorkSchedule schedule = scheduleRepository
                .findByCollaboratorIdIncludingDeleted(dto.collaboratorId()).stream()
                .filter(s -> s.getDayOfWeek() == dto.dayOfWeek())
                .findFirst()
                .orElse(null);

        if (schedule != null && schedule.getDeletedAt() == null) {
            throw new BusinessException(
                    "Este colaborador já possui horário cadastrado para " + dto.dayOfWeek().getLabel() + ".");
        }
        if (schedule == null) {
            schedule = new CollaboratorWorkSchedule();
            schedule.setCollaborator(collaborator);
            schedule.setDayOfWeek(dto.dayOfWeek());
        } else {
            // Reactivate the soft-deleted day instead of violating the unique index.
            schedule.setDeletedAt(null);
        }

        schedule.setAllowedStartTime(dto.allowedStartTime());
        schedule.setAllowedEndTime(dto.allowedEndTime());
        schedule.setActive(true);

        return new CollaboratorWorkScheduleResponseDTO(scheduleRepository.save(schedule));
    }

    @Transactional
    public CollaboratorWorkScheduleResponseDTO update(Long id, CollaboratorWorkScheduleDTO dto) {
        CollaboratorWorkSchedule schedule = findEntityById(Objects.requireNonNull(id));
        validateAllowedWindow(dto.allowedStartTime(), dto.allowedEndTime());

        Collaborator collaborator = collaboratorRepository.findByIdScoped(Objects.requireNonNull(dto.collaboratorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));

        schedule.setCollaborator(collaborator);
        schedule.setDayOfWeek(dto.dayOfWeek());
        schedule.setAllowedStartTime(dto.allowedStartTime());
        schedule.setAllowedEndTime(dto.allowedEndTime());
        schedule.setActive(dto.active());

        return new CollaboratorWorkScheduleResponseDTO(scheduleRepository.save(schedule));
    }

    @Transactional
    public void softDelete(Long id) {
        CollaboratorWorkSchedule schedule = findEntityById(id);
        schedule.setActive(false);
        schedule.setDeletedAt(LocalDateTime.now());
        scheduleRepository.save(schedule);
    }

    /**
     * O horário não tem {@code company_id} próprio: ele pertence à empresa do
     * colaborador. Por isso o predicado de tenant navega até lá, em vez de usar o
     * caminho direto das demais entidades.
     */
    private Specification<CollaboratorWorkSchedule> scopedToCompany(Specification<CollaboratorWorkSchedule> spec) {
        return Specification.allOf(spec,
                TenantSpecifications.<CollaboratorWorkSchedule>currentCompany("collaborator", "company"));
    }

    private CollaboratorWorkSchedule findEntityById(Long id) {
        CollaboratorWorkSchedule schedule = scheduleRepository
                .findOne(scopedToCompany(TenantSpecifications.hasId(Objects.requireNonNull(id))))
                .orElseThrow(() -> new ResourceNotFoundException("Horário de trabalho não encontrado"));
        requireCollaboratorAccess(schedule.getCollaborator().getId());
        return schedule;
    }

    private void requireCollaboratorAccess(Long collaboratorId) {
        if (!access.operatesCompany()
                && !access.canAccessCollaborator(collaboratorId)) {
            throw new AccessDeniedException("Acesso negado. Você só pode acessar seus próprios horários.");
        }
    }

    /** Rejects duplicated weekdays up front, so the unique index never has to. */
    private Map<WorkDay, WorkScheduleItemDTO> indexByDay(List<WorkScheduleItemDTO> items) {
        Map<WorkDay, WorkScheduleItemDTO> byDay = new EnumMap<>(WorkDay.class);
        EnumSet<WorkDay> duplicated = EnumSet.noneOf(WorkDay.class);
        for (WorkScheduleItemDTO item : items) {
            if (byDay.put(item.dayOfWeek(), item) != null) {
                duplicated.add(item.dayOfWeek());
            }
        }
        if (!duplicated.isEmpty()) {
            throw new BusinessException("Dia da semana repetido na requisição: " + duplicated);
        }
        return byDay;
    }

    private void validateAllowedWindow(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessException("O término da janela permitida deve ser posterior ao início");
        }
    }
}
