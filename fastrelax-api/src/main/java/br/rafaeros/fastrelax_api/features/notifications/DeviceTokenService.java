package br.rafaeros.fastrelax_api.features.notifications;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.security.Principals;
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
        DeviceToken deviceToken = findExisting(dto)
                .map(existing -> reuseOrReplace(existing, collaborator))
                .orElseGet(DeviceToken::new);

        if (deviceToken.getCompany() == null) {
            // A empresa vem do colaborador, não do contexto — é dele que o aparelho é.
            deviceToken.setCompany(collaborator.getCompany());
        }
        deviceToken.setToken(dto.token());
        deviceToken.setPushSubscription(dto.pushSubscription());
        deviceToken.setPlatform(dto.platform());
        deviceToken.setCollaborator(collaborator);
        deviceToken.setActive(true);

        return new DeviceTokenResponseDTO(deviceTokenRepository.save(deviceToken));
    }

    /**
     * Aparelho que mudou de dono.
     *
     * <p>
     * Dentro da mesma empresa basta trocar o colaborador na linha existente. Se o
     * dono novo é de outra empresa, a linha não pode ser reaproveitada: o
     * {@code company_id} é imutável de propósito, para que nenhum registro mude de
     * tenant por acidente. A linha antiga é apagada — o aparelho não é mais
     * daquela pessoa, e o índice único do token não admite duas — e o registro
     * nasce de novo no tenant certo.
     */
    private DeviceToken reuseOrReplace(DeviceToken existing, Collaborator collaborator) {
        if (java.util.Objects.equals(existing.companyId(), collaborator.companyId())) {
            return existing;
        }
        deviceTokenRepository.delete(existing);
        deviceTokenRepository.flush();
        return new DeviceToken();
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
        return Principals.requireCollaborator();
    }
}
