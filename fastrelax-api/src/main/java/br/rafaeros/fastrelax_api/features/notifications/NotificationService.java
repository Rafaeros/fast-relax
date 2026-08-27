package br.rafaeros.fastrelax_api.features.notifications;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.notifications.dtos.NotificationResponseDTO;
import br.rafaeros.fastrelax_api.features.notifications.push.PushDispatcher;
import br.rafaeros.fastrelax_api.features.notifications.push.PushMessage;
import lombok.RequiredArgsConstructor;

/**
 * Porta única de saída de avisos ao colaborador.
 *
 * <p>
 * Grava primeiro, entrega depois. A ordem não é detalhe: push é entrega
 * best-effort e some sem deixar rastro se o aparelho estiver desligado ou a
 * permissão tiver sido revogada. Com o registro no banco, a central de
 * notificações do app mostra o aviso na próxima abertura mesmo que nenhum push
 * tenha chegado.
 *
 * <p>
 * Quem chama não sabe se o destino é Android ou navegador — essa escolha é do
 * {@link PushDispatcher}. Um canal novo (e-mail, WhatsApp) entra ali dentro sem
 * tocar nas regras de sessão.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final PushDispatcher pushDispatcher;

    /**
     * Registra o aviso e dispara o push para todos os aparelhos do colaborador.
     *
     * @param data carga opcional para o clique abrir a tela certa
     */
    @Transactional
    public Notification notifyCollaborator(Long collaboratorId, NotificationType type, String title, String body,
            Map<String, Object> data, String url) {
        Collaborator collaborator = collaboratorRepository.findById(Objects.requireNonNull(collaboratorId))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));

        Notification notification = new Notification();
        // A empresa vem do colaborador: este metodo tambem e chamado pelos jobs, que
        // rodam sem tenant no contexto e nao teriam de onde tirar o dono do aviso.
        notification.setCompany(collaborator.getCompany());
        notification.setCollaborator(collaborator);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(data);

        Notification saved = notificationRepository.save(notification);

        // A tag agrupa por tipo: um lembrete novo substitui o anterior na bandeja
        // em vez de empilhar avisos repetidos sobre a mesma massagem.
        pushDispatcher.dispatch(collaboratorId,
                new PushMessage(title, body, url, type.name(), Map.of("notificationId", String.valueOf(saved.getId()))));

        return saved;
    }

    public Page<NotificationResponseDTO> listMine(Pageable pageable) {
        Long collaboratorId = requireLoggedCollaboratorId();
        return notificationRepository
                .findByCollaboratorIdOrderByCreatedAtDesc(collaboratorId, Objects.requireNonNull(pageable))
                .map(notification -> new NotificationResponseDTO(notification));
    }

    /** Contador do sininho — chamado com frequência, então não carrega o corpo. */
    public long countMyUnread() {
        return notificationRepository.countByCollaboratorIdAndReadAtIsNull(requireLoggedCollaboratorId());
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long id) {
        Long collaboratorId = requireLoggedCollaboratorId();

        // Escopada: um id de outra empresa nem chega à checagem de dono, responde
        // 404 como se não existisse.
        Notification notification = notificationRepository.findByIdScoped(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));

        if (!notification.getCollaborator().getId().equals(collaboratorId)) {
            throw new AccessDeniedException("Acesso negado. Você só pode ler as suas próprias notificações.");
        }

        // Reler não move a data: o interessante é quando o aviso foi visto pela
        // primeira vez.
        if (!notification.isRead()) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }

        return new NotificationResponseDTO(notification);
    }

    /** @return quantas ficaram marcadas */
    @Transactional
    public int markAllAsRead() {
        return notificationRepository.markAllAsRead(requireLoggedCollaboratorId(), LocalDateTime.now());
    }

    private Long requireLoggedCollaboratorId() {
        return Principals.requireCollaborator().getId();
    }
}
