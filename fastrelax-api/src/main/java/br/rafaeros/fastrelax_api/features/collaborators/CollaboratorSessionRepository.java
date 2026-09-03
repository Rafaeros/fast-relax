package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;

/**
 * Consultas de sessão.
 *
 * <p>
 * As que recebem {@code companyId} são as do fluxo de requisição. As que não
 * recebem são das rotinas de fundo, que varrem todas as empresas de propósito —
 * lembrete e expiração não têm por que rodar uma vez por cliente.
 */
public interface CollaboratorSessionRepository extends CompanyScopedRepository<CollaboratorSession> {

    List<CollaboratorSession> findByCollaboratorId(Long collaboratorId);

    /** Espelha o índice parcial {@code uq_collaborator_active_session}. */
    Optional<CollaboratorSession> findByCollaboratorIdAndStatusIn(Long collaboratorId, List<SessionStatus> statuses);

    /** Base das agregações do painel. */
    List<CollaboratorSession> findByCompanyIdAndSessionDateBetween(Long companyId, LocalDate from, LocalDate to);

    /** Ocupações de todo o período numa consulta só, em vez de uma por dia da grade. */
    List<CollaboratorSession> findByCompanyIdAndSessionDateBetweenAndStatusIn(Long companyId, LocalDate from,
            LocalDate to, List<SessionStatus> statuses);

    /**
     * Quantas sessões ativas da empresa ocupam a faixa pedida.
     *
     * <p>
     * Antes isto era um {@code exists}: havia uma cadeira só, então qualquer
     * sobreposição era conflito. Com várias cadeiras por empresa, o horário só
     * está cheio quando as sessões simultâneas igualam o número de cadeiras — e é
     * por isso que a resposta virou contagem.
     *
     * <p>
     * Intervalos são semiabertos: encostar (12:05 logo após 12:00–12:05) não
     * conta como sobreposição.
     *
     * @param excludeId id a ignorar ao reagendar; use um valor inexistente (-1) ao criar
     */
    @Query("""
            SELECT COUNT(s) FROM CollaboratorSession s
            WHERE s.company.id = :companyId
              AND s.sessionDate = :sessionDate
              AND s.status IN :statuses
              AND s.id <> :excludeId
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    long countOverlapping(@Param("companyId") Long companyId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("excludeId") Long excludeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Igual a {@link #countOverlapping}, mas por cadeira específica, não por
     * capacidade agregada da empresa — é o que sustenta a folga de
     * estabilização, que é uma propriedade da cadeira (relé + poltrona), não
     * do total de cadeiras.
     *
     * @param excludeId id a ignorar ao reagendar; use um valor inexistente (-1) ao criar
     */
    @Query("""
            SELECT COUNT(s) FROM CollaboratorSession s
            WHERE s.chair.id = :chairId
              AND s.sessionDate = :sessionDate
              AND s.status IN :statuses
              AND s.id <> :excludeId
              AND s.startTime < :endTime
              AND s.endTime > :startTime
            """)
    long countOverlappingChair(@Param("chairId") Long chairId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("excludeId") Long excludeId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * Candidatas a expiração, de todas as empresas: sessões ainda ativas de hoje
     * ou de dias anteriores.
     */
    List<CollaboratorSession> findByCompanyIdAndStatusInAndSessionDateLessThanEqual(Long companyId,
            List<SessionStatus> statuses, LocalDate sessionDate);

    /**
     * Sessões agendadas que começam dentro da janela pedida, em qualquer empresa.
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

    /**
     * Sessão em andamento numa cadeira específica, se houver.
     *
     * <p>
     * Usada quando a Physical desativa a cadeira: sem isto não haveria como
     * saber se tem alguém sentado nela naquele instante para encerrar a sessão
     * junto do corte de energia.
     */
    Optional<CollaboratorSession> findByChairIdAndStatus(Long chairId, SessionStatus status);
}
