package br.rafaeros.fastrelax_api.core.config;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Fixa o fuso da JVM.
 *
 * <p>
 * Toda regra de horário do sistema usa {@code LocalDate.now()} e
 * {@code LocalTime.now()}, que leem o fuso do sistema operacional. Em container
 * ou nuvem esse padrão costuma ser UTC, e aí meio-dia em Brasília chega como
 * 15:00 — a janela de almoço rejeitaria agendamentos válidos, o início de sessão
 * seria recusado e o job expiraria sessões antes da hora.
 */
@Configuration
public class ApplicationTimeZoneConfig {

    private static final Logger log = LoggerFactory.getLogger(ApplicationTimeZoneConfig.class);

    @Value("${app.timezone:America/Sao_Paulo}")
    private String timezone;

    @PostConstruct
    public void applyTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        log.info("Fuso horário da aplicação fixado em {}", timezone);
    }
}
