package br.rafaeros.fastrelax_api.features.chairs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.features.chairs.ChairCommandResult.Outcome;
import lombok.RequiredArgsConstructor;

/**
 * Traduz intenções de sessão em comandos de cadeira.
 *
 * <p>
 * É a fronteira entre a regra de negócio e o hardware: quem cuida de sessão pede
 * "ligue para esta sessão" e não sabe que existe HTTP, IP ou relé. Trocar o
 * transporte para MQTT altera apenas {@link ChairClient} e esta classe.
 *
 * <p>
 * Também é onde o estado do dispositivo volta para o banco: uma recusa por
 * estabilização traz o prazo que o firmware está contando, e guardá-lo evita
 * gastar a próxima viagem para ouvir o mesmo não.
 */
@Service
@RequiredArgsConstructor
public class ChairCommandService {

    private static final Logger log = LoggerFactory.getLogger(ChairCommandService.class);

    private final ChairService chairService;
    private final ChairClient chairClient;

    /**
     * Aloca uma cadeira online e liga o aparelho.
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

        ChairCommandResult result = chairClient.start(chair, sessionId, durationSeconds);
        if (!result.isAccepted()) {
            recordState(chair, result);
            throw new BusinessException(result.message());
        }
        return chair;
    }

    /**
     * Aciona uma cadeira específica, sem sessão, para conferir a instalação
     * elétrica.
     *
     * <p>
     * Diferente de {@link #startFor}, não escolhe a cadeira nem exige que ela
     * esteja online no critério de heartbeat: o objetivo é justamente descobrir
     * por que uma cadeira não responde. Basta ter IP conhecido.
     */
    public void testRelay(Chair chair, int durationSeconds) {
        ChairCommandResult result = chairClient.testRelay(chair, durationSeconds);

        if (!result.isAccepted()) {
            recordState(chair, result);
            throw new BusinessException(result.message());
        }
        log.info("Teste de relé disparado na cadeira {} por {}s", chair.getName(), durationSeconds);
    }

    /**
     * Desliga antes do previsto.
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

        ChairCommandResult result = chairClient.stop(chair, sessionId);

        if (result.isAccepted()) {
            // O pulso de desligar acabou de sair, então a cadeira entrou em
            // estabilização agora. Registrar aqui é o que faz a próxima sessão
            // ser recusada cedo, em vez de bater no 409 do dispositivo.
            chairService.markCoolingDown(chair, chairService.getCooldownSeconds());
            return;
        }

        recordState(chair, result);

        if (result.deviceAnswered()) {
            log.warn("Cadeira {} recusou o desligamento da sessão {}: {}",
                    chair.getName(), sessionId, result.message());
            return;
        }

        // Silêncio do dispositivo: rede caída ou firmware travado. Não dá para
        // afirmar que a cadeira desligou — o que segura o caso é a contagem
        // local do próprio ESP32, que encerra ao fim da duração.
        log.error("Cadeira {} não respondeu ao desligamento da sessão {}; "
                + "verifique rede e dispositivo. O firmware encerra sozinho ao fim da duração.",
                chair.getName(), sessionId);
    }

    /**
     * Guarda no banco o que a resposta revelou sobre a cadeira.
     *
     * <p>
     * Só a recusa por estabilização carrega estado aproveitável: ela vem com o
     * prazo que o firmware está contando. As demais não dizem nada sobre a
     * janela, e o silêncio menos ainda — apagar a janela conhecida por causa de
     * um timeout seria trocar informação boa por nenhuma.
     */
    private void recordState(Chair chair, ChairCommandResult result) {
        if (result.outcome() == Outcome.COOLING_DOWN) {
            chairService.markCoolingDown(chair, result.retryAfterSeconds());
        }
    }
}
