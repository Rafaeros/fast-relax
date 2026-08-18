package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaboratorSessionRepository
        extends JpaRepository<CollaboratorSession, Long>, JpaSpecificationExecutor<CollaboratorSession> {

    List<CollaboratorSession> findByCollaboratorId(Long collaboratorId);

    /** Mirrors the partial unique index {@code uq_collaborator_active_session}. */
    Optional<CollaboratorSession> findByCollaboratorIdAndStatusIn(Long collaboratorId, List<SessionStatus> statuses);

    List<CollaboratorSession> findBySessionDateAndStatusIn(LocalDate sessionDate, List<SessionStatus> statuses);

    /** Base das agregações do painel do RH. */
    List<CollaboratorSession> findBySessionDateBetween(LocalDate from, LocalDate to);

    /** Ocupações de todo o período numa consulta só, em vez de uma por dia da grade. */
    List<CollaboratorSession> findBySessionDateBetweenAndStatusIn(LocalDate from, LocalDate to,
            List<SessionStatus> statuses);

    /**
     * Só existe um recurso de atendimento, então duas sessões ativas nunca podem
     * se sobrepor no tempo — nem entre colaboradores diferentes. Intervalos são
     * semiabertos: encostar (12:05 logo após 12:00–12:05) não é conflito.
     *
     * @param excludeId id a ignorar ao reagendar; use um valor inexistente (-1) ao criar
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM CollaboratorSession s
            WHERE s.sessionDate = :sessionDate
              AND s.status IN :statuses
              AND s.id <> :excludeId
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    boolean existsOverlapping(@Param("sessionDate") LocalDate sessionDate,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("excludeId") Long excludeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /** Candidatas a expiração: sessões ainda ativas de hoje ou de dias anteriores. */
    List<CollaboratorSession> findByStatusInAndSessionDateLessThanEqual(List<SessionStatus> statuses,
            LocalDate sessionDate);

    /**
     * Sessões agendadas que começam dentro da janela pedida.
     *
     * <p>
     * Nativa porque a comparação é com o instante do início — data mais hora —, e
     * JPQL não soma os dois. No Postgres {@code date + time} já é um timestamp.
     *
     * <p>
     * O piso em {@code now} exclui o que já passou: sessão vencida não recebe
     * lembrete, recebe o aviso de expiração.
     */
    @Query(value = """
            SELECT * FROM collaborator_sessions s
            WHERE s.status = 'SCHEDULED'
              AND (s.session_date + s.start_time) > :now
              AND (s.session_date + s.start_time) <= :limit
            """, nativeQuery = true)
    List<CollaboratorSession> findScheduledStartingBetween(@Param("now") LocalDateTime now,
            @Param("limit") LocalDateTime limit);

    /** Agenda de um dia inteiro, base do resumo enviado na véspera. */
    List<CollaboratorSession> findBySessionDateAndStatus(LocalDate sessionDate, SessionStatus status);
}
