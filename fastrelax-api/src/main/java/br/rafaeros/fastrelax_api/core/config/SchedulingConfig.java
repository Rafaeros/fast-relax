package br.rafaeros.fastrelax_api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Pool dedicado para as tarefas agendadas.
 *
 * <p>
 * O padrão do Spring é uma única thread para todos os {@code @Scheduled}: o
 * monitor de cadeiras e a expiração de sessões ficariam em fila, e um HTTP lento
 * para o ESP32 atrasaria a expiração. Com pool próprio cada job corre por conta.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Uma thread por tarefa agendada: monitor de cadeiras, expiração de
        // sessões, lembretes rolantes e resumo diário. Com o pool menor que a
        // contagem, uma tarefa lenta atrasaria outra sem relação com ela.
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("fastrelax-job-");
        // Espera as tarefas em andamento no shutdown: interromper no meio poderia
        // deixar uma sessão marcada sem o relé ter sido desligado.
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        return scheduler;
    }
}
