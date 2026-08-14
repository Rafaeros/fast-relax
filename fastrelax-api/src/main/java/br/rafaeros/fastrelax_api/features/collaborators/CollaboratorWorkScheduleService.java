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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CollaboratorSecurity collaboratorSecurity;

    public Page<CollaboratorWorkScheduleResponseDTO> findAll(CollaboratorWorkScheduleFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        // A logged collaborator may only see their own schedules, so their id
        // overrides any collaboratorId supplied in the filter.
        Long collaboratorId = dto != null ? dto.collaboratorId() : null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Collaborator loggedCollab) {
            collaboratorId = loggedCollab.getId();
        }

        Specification<CollaboratorWorkSchedule> spec = Specification.allOf(
                CollaboratorWorkScheduleSpecifications.hasDayOfWeek(dto != null ? dto.dayOfWeek() : null),
                CollaboratorWorkScheduleSpecifications.hasActive(dto != null ? dto.active() : null),
                CollaboratorWorkScheduleSpecifications.hasCollaborator(collaboratorId));

        return scheduleRepository.findAll(spec, Objects.requireNonNull(pageable))
                .map(CollaboratorWorkScheduleResponseDTO::new);
    }

    public CollaboratorWorkScheduleResponseDTO findById(Long id) {
        return new CollaboratorWorkScheduleResponseDTO(findEntityById(id));
    }

    /** Semana do colaborador logado, sem precisar informar o id. */
    public List<CollaboratorWorkScheduleResponseDTO> findMyWeeklySchedule() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Collaborator logged)) {
            throw new AccessDeniedException("Rota disponível apenas para colaboradores autenticados");
        }
        return findWeeklySchedule(logged.getId());
    }

    /** Every active weekday configured for a collaborator, ordered Monday to Friday. */
    public List<CollaboratorWorkScheduleResponseDTO> findWeeklySchedule(Long collaboratorId) {
        requireCollaboratorAccess(collaboratorId);
        return scheduleRepository.findByCollaboratorId(Objects.requireNonNull(collaboratorId)).stream()
                .sorted((a, b) -> a.getDayOfWeek().compareTo(b.getDayOfWeek()))
                .map(CollaboratorWorkScheduleResponseDTO::new)
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
        Collaborator collaborator = collaboratorRepository.findById(Objects.requireNonNull(collaboratorId))
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
                .map(CollaboratorWorkScheduleResponseDTO::new)
                .toList();
    }

    @Transactional
    public CollaboratorWorkScheduleResponseDTO create(CollaboratorWorkScheduleDTO dto) {
        Collaborator collaborator = collaboratorRepository.findById(Objects.requireNonNull(dto.collaboratorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
        validateAllowedWindow(dto.allowedStartTime(), dto.allowedEndTime());

        CollaboratorWorkSchedule schedule = scheduleRepository
                .findByCollaboratorIdIncludingDeleted(dto.collaboratorId()).stream()
                .filter(s -> s.getDayOfWeek() == dto.dayOfWeek())
                .findFirst()
                .orElse(null);

        if (schedule != null && schedule.getDeletedAt() == null) {
            throw new BusinessException("Este colaborador já possui horário cadastrado para " + dto.dayOfWeek());
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

        Collaborator collaborator = collaboratorRepository.findById(Objects.requireNonNull(dto.collaboratorId()))
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

    private CollaboratorWorkSchedule findEntityById(Long id) {
        CollaboratorWorkSchedule schedule = scheduleRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Horário de trabalho não encontrado"));
        requireCollaboratorAccess(schedule.getCollaborator().getId());
        return schedule;
    }

    private void requireCollaboratorAccess(Long collaboratorId) {
        if (!collaboratorSecurity.hasAdminOrRhAccess()
                && !collaboratorSecurity.canAccessCollaborator(collaboratorId)) {
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
