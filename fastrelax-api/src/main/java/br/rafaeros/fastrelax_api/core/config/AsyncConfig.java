package br.rafaeros.fastrelax_api.core.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool para o envio de push.
 *
 * <p>
 * O envio sai da thread da requisição de propósito: o colaborador está
 * esperando a resposta de "agendar" ou "iniciar", e o serviço de push é uma
 * chamada de rede a terceiros que pode demorar ou falhar. Nada disso pode
 * atrasar — nem derrubar — a operação que já foi gravada.
 *
 * <p>
 * A fila é limitada e a política de rejeição devolve a tarefa para quem chamou:
 * numa rajada, é melhor um envio sair devagar na própria thread do que a fila
 * crescer sem limite consumindo memória.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("fastrelax-push-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
