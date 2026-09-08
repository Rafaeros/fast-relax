package br.rafaeros.fastrelax_api.features.evaluation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;

public interface EvaluationRepository extends CompanyScopedRepository<Evaluation> {

    /**
     * A listagem monta o DTO com nome do colaborador, cadeira e data da sessão.
     * Sem o grafo, cada linha da página dispararia as suas próprias consultas de
     * proxy — o N+1 clássico de tela de listagem.
     */
    @Override
    @EntityGraph(attributePaths = { "collaborator", "collaborator.department", "session", "session.chair" })
    @NonNull
    Page<Evaluation> findAll(Specification<Evaluation> spec, @NonNull Pageable pageable);

    boolean existsBySessionId(Long sessionId);

    Optional<Evaluation> findBySessionId(Long sessionId);

    /**
     * Massagens concluídas do colaborador que ainda não receberam nota.
     *
     * <p>
     * Devolve sessão, não avaliação, porque é o que o app precisa para reabrir o
     * modal quando a pessoa fecha sem responder e volta depois. Fica neste
     * repositório de propósito: a dependência entre os dois módulos já aponta de
     * avaliação para sessão, e declará-la no sentido contrário faria o cadastro de
     * sessões passar a conhecer avaliação.
     *
     * @param since piso pela hora de término — sem ele, uma massagem de meses atrás
     *              voltaria a pedir nota toda vez que o app abrisse
     */
    @Query("""
            SELECT s FROM CollaboratorSession s
            WHERE s.collaborator.id = :collaboratorId
              AND s.status = br.rafaeros.fastrelax_api.features.collaborators.SessionStatus.DONE
              AND s.finishedAt >= :since
              AND NOT EXISTS (SELECT 1 FROM Evaluation e WHERE e.session = s)
            ORDER BY s.finishedAt DESC
            """)
    List<CollaboratorSession> findPendingSessions(@Param("collaboratorId") Long collaboratorId,
            @Param("since") LocalDateTime since, Pageable pageable);
}
