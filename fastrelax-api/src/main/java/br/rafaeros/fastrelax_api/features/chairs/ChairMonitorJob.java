package br.rafaeros.fastrelax_api.features.chairs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Vigia a presença das cadeiras.
 *
 * <p>
 * O estado online é derivado do último heartbeat, então a consulta sempre reflete
 * a realidade sem este job. O que ele acrescenta é perceber a <em>transição</em>:
 * sem alguém olhando periodicamente, uma cadeira que caiu só seria notada quando
 * um colaborador tentasse iniciar a sessão e recebesse erro.
 */
@Component
@RequiredArgsConstructor
public class ChairMonitorJob {

    private static final Logger log = LoggerFactory.getLogger(ChairMonitorJob.class);

    private final ChairRepository chairRepository;

    @Value("${app.chair.offline-after-seconds:180}")
    private int offlineAfterSeconds;

    /** Último estado conhecido por cadeira, só para detectar a virada. */
    private final Map<Long, Boolean> lastKnownState = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${app.chair.monitor-interval-ms:30000}")
    public void checkChairs() {
        for (Chair chair : chairRepository.findByActiveTrue()) {
            boolean online = chair.isOnline(offlineAfterSeconds);
            Boolean previous = lastKnownState.put(chair.getId(), online);

            if (previous != null && previous != online) {
                if (online) {
                    log.info("Cadeira {} ({}) voltou a responder", chair.getName(), chair.getMacAddress());
                } else {
                    log.warn("Cadeira {} ({}) está offline: sem heartbeat há mais de {}s",
                            chair.getName(), chair.getMacAddress(), offlineAfterSeconds);
                }
            }
        }
    }
}
