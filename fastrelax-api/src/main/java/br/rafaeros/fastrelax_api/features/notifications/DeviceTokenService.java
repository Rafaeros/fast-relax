package br.rafaeros.fastrelax_api.features.notifications;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.notifications.dtos.DeviceTokenResponseDTO;
import br.rafaeros.fastrelax_api.features.notifications.dtos.RegisterDeviceTokenDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Registra ou reativa o token do aparelho do colaborador logado.
     *
     * <p>
     * Se o token já existir vinculado a outra pessoa, o dono é trocado: em
     * aparelho compartilhado, o push tem que seguir quem está logado agora, senão
     * a notificação vaza para o usuário anterior.
     */
    @Transactional
    public DeviceTokenResponseDTO register(RegisterDeviceTokenDTO dto) {
        Collaborator collaborator = requireLoggedCollaborator();

        DeviceToken deviceToken = deviceTokenRepository.findByToken(dto.token())
                .orElseGet(DeviceToken::new);

        deviceToken.setToken(dto.token());
        deviceToken.setPlatform(dto.platform());
        deviceToken.setCollaborator(collaborator);
        deviceToken.setActive(true);

        return new DeviceTokenResponseDTO(deviceTokenRepository.save(deviceToken));
    }

    /** Logout do aparelho: para de receber push sem apagar o histórico. */
    @Transactional
    public void unregister(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
        });
    }

    public List<DeviceTokenResponseDTO> listMine() {
        Collaborator collaborator = requireLoggedCollaborator();
        return deviceTokenRepository.findByCollaboratorIdAndActiveTrue(collaborator.getId()).stream()
                .map(DeviceTokenResponseDTO::new)
                .toList();
    }

    /** Destinos de push de um colaborador — ponto de entrada para o envio via FCM. */
    public List<String> activeTokensFor(Long collaboratorId) {
        return deviceTokenRepository.findByCollaboratorIdAndActiveTrue(collaboratorId).stream()
                .map(deviceToken -> deviceToken.getToken())
                .toList();
    }

    private Collaborator requireLoggedCollaborator() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Collaborator logged)) {
            throw new AccessDeniedException("Rota disponível apenas para colaboradores autenticados");
        }
        return logged;
    }
}
