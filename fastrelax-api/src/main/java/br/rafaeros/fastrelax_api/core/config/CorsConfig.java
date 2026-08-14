package br.rafaeros.fastrelax_api.core.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS para acesso pelo navegador.
 *
 * <p>
 * O painel chama a API pelo servidor do Next, então o caminho normal nem passa
 * por aqui. Esta configuração cobre chamadas feitas direto do browser e
 * ferramentas de rede. Apps móveis não são afetados por CORS.
 *
 * <p>
 * As origens vêm de {@code CORS_ALLOWED_ORIGINS} no {@code .env} e devem
 * conter apenas a máquina local e faixas privadas — a API não é exposta na
 * internet. Curinga é usado no host e na porta, nunca a origem {@code *}
 * sozinha: com credenciais habilitadas o navegador rejeita a combinação.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(parseOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Content-Disposition precisa ser exposto para o painel ler o nome do
        // arquivo nos downloads de planilha.
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(origin -> origin.trim())
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
