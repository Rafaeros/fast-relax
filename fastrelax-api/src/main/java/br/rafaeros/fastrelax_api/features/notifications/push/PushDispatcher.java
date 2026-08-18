package br.rafaeros.fastrelax_api.features.notifications.push;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.features.notifications.DeviceToken;
import br.rafaeros.fastrelax_api.features.notifications.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;

/**
 * Espalha um aviso por todos os aparelhos ativos do colaborador.
 *
 * <p>
 * Assíncrono de propósito: quem chama está no meio de uma operação que o
 * colaborador está esperando (agendar, iniciar, cancelar). Um serviço de push
 * lento não pode segurar essa resposta, e uma falha de entrega não pode
 * desfazer o que já foi gravado.
 */
@Component
@RequiredArgsConstructor
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final List<PushProvider> providers;

    @Async
    @Transactional
    public void dispatch(Long collaboratorId, PushMessage message) {
        List<DeviceToken> destinations = deviceTokenRepository.findByCollaboratorIdAndActiveTrue(collaboratorId);
        if (destinations.isEmpty()) {
            return;
        }

        for (DeviceToken destination : destinations) {
            providers.stream()
                    .filter(provider -> provider.supports(destination.getPlatform()))
                    .findFirst()
                    .ifPresentOrElse(
                            provider -> deliver(provider, destination, message),
                            () -> log.warn("Sem provedor de push para a plataforma {}", destination.getPlatform()));
        }
    }

    private void deliver(PushProvider provider, DeviceToken destination, PushMessage message) {
        PushResult result = provider.send(destination, message);

        // Destino morto é desativado na hora: mantê-lo ativo faria toda
        // notificação futura gastar uma chamada de rede para receber o mesmo
        // 410, e a lista de aparelhos do colaborador só cresceria com lixo.
        if (result == PushResult.GONE) {
            destination.setActive(false);
            deviceTokenRepository.save(destination);
        }
    }
}
