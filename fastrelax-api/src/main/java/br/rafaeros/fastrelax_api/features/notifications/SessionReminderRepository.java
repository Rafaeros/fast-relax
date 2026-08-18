package br.rafaeros.fastrelax_api.features.notifications;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionReminderRepository extends JpaRepository<SessionReminder, Long> {

    /**
     * Quais das sessões candidatas já receberam esta faixa.
     *
     * <p>
     * Uma consulta para o lote inteiro em vez de uma por sessão: o job roda de
     * minuto em minuto, e o custo tem que ser de um índice, não de N idas ao
     * banco.
     */
    @Query("SELECT r.sessionId FROM SessionReminder r WHERE r.kind = :kind AND r.sessionId IN :sessionIds")
    List<Long> findAlreadySent(@Param("kind") String kind, @Param("sessionIds") Collection<Long> sessionIds);
}
