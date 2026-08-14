package br.rafaeros.fastrelax_api.features.chairs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;

/**
 * Traduz intenções de sessão em comandos de cadeira.
 *
 * <p>
 * É a fronteira entre a regra de negócio e o hardware: quem cuida de sessão pede
 * "ligue para esta sessão" e não sabe que existe HTTP, IP ou relé. Trocar o
 * transporte para MQTT altera apenas {@link ChairClient} e esta classe.
 */
@Service
@RequiredArgsConstructor
public class ChairCommandService {

    private static final Logger log = LoggerFactory.getLogger(ChairCommandService.class);

    private final ChairService chairService;
    private final ChairClient chairClient;

    /**
     * Aloca uma cadeira online e liga o relé.
     *
     * <p>
     * Falha em vez de seguir adiante: iniciar a sessão sem a cadeira ter ligado
     * deixaria o colaborador em frente a um equipamento parado, com o app dizendo
     * que está em andamento.
     *
     * @return a cadeira que atendeu, para registrar na sessão
     */
    public Chair startFor(Long sessionId, int durationSeconds) {
        Chair chair = chairService.findAvailableChair();

        if (!chairClient.start(chair, sessionId, durationSeconds)) {
            throw new BusinessException(
                    "Não foi possível acionar a cadeira. Tente novamente ou procure o RH.");
        }
        return chair;
    }

    /**
     * Desliga o relé antes do previsto.
     *
     * <p>
     * Não lança: cancelamento e expiração precisam concluir mesmo com a cadeira
     * fora do ar. O ESP32 desliga sozinho ao fim da duração, então uma falha aqui
     * atrasa o desligamento, não o impede.
     */
    public void stopFor(Chair chair, Long sessionId) {
        if (chair == null) {
            return;
        }
        if (!chairClient.stop(chair, sessionId)) {
            log.warn("Cadeira {} não confirmou o desligamento da sessão {}; "
                    + "o próprio dispositivo encerra ao fim da duração", chair.getName(), sessionId);
        }
    }
}
