package br.rafaeros.fastrelax_api.features.notifications;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;

public interface NotificationRepository extends CompanyScopedRepository<Notification> {

    Page<Notification> findByCollaboratorIdOrderByCreatedAtDesc(Long collaboratorId, Pageable pageable);

    long countByCollaboratorIdAndReadAtIsNull(Long collaboratorId);

    /**
     * Marca tudo de uma vez em uma instrução: carregar a lista só para setar uma
     * data faria N updates e traria o corpo das mensagens à toa.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now "
            + "WHERE n.collaborator.id = :collaboratorId AND n.readAt IS NULL")
    int markAllAsRead(@Param("collaboratorId") Long collaboratorId, @Param("now") LocalDateTime now);
}
