package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaboratorWorkScheduleRepository
        extends JpaRepository<CollaboratorWorkSchedule, Long>, JpaSpecificationExecutor<CollaboratorWorkSchedule> {

    List<CollaboratorWorkSchedule> findByCollaboratorId(Long collaboratorId);

    /** Janela de horário permitido vigente do colaborador num dia da semana. */
    Optional<CollaboratorWorkSchedule> findByCollaboratorIdAndDayOfWeekAndActiveTrue(Long collaboratorId,
            WorkDay dayOfWeek);

    // Native query bypasses @SQLRestriction("deleted_at IS NULL") so the weekly
    // replace can reactivate soft-deleted days instead of hitting the unique index.
    @Query(value = "SELECT * FROM collaborator_work_schedules WHERE collaborator_id = :collaboratorId",
            nativeQuery = true)
    List<CollaboratorWorkSchedule> findByCollaboratorIdIncludingDeleted(@Param("collaboratorId") Long collaboratorId);
}
