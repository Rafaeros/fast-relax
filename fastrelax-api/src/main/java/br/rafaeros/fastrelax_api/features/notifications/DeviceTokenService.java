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

        // Cada tecnologia tem sua identidade: FCM identifica pelo token, Web Push
        // pelo endpoint da inscrição. Procurar pela chave certa é o que faz o
        // registro repetido atualizar a linha em vez de duplicá-la.
        DeviceToken deviceToken = findExisting(dto).orElseGet(() -> new DeviceToken());

        deviceToken.setToken(dto.token());
        deviceToken.setPushSubscription(dto.pushSubscription());
        deviceToken.setPlatform(dto.platform());
        deviceToken.setCollaborator(collaborator);
        deviceToken.setActive(true);

        return new DeviceTokenResponseDTO(deviceTokenRepository.save(deviceToken));
    }

    private java.util.Optional<DeviceToken> findExisting(RegisterDeviceTokenDTO dto) {
        if (dto.platform() == DeviceToken.Platform.WEB) {
            return deviceTokenRepository.findBySubscriptionEndpoint(dto.pushSubscription().endpoint());
        }
        return deviceTokenRepository.findByToken(dto.token());
    }

    /** Logout do aparelho: para de receber push sem apagar o histórico. */
    @Transactional
    public void unregister(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
        });
    }

    /**
     * Desfaz a inscrição do navegador.
     *
     * <p>
     * Separado do {@link #unregister(String)} porque o navegador não tem token
     * para devolver: o que ele conhece da própria inscrição é o endpoint.
     */
    @Transactional
    public void unregisterSubscription(String endpoint) {
        deviceTokenRepository.findBySubscriptionEndpoint(endpoint).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
        });
    }

    public List<DeviceTokenResponseDTO> listMine() {
        Collaborator collaborator = requireLoggedCollaborator();
        return deviceTokenRepository.findByCollaboratorIdAndActiveTrue(collaborator.getId()).stream()
                .map(token -> new DeviceTokenResponseDTO(token))
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
