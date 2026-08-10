package br.rafaeros.fastrelax_api.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentação em {@code /swagger-ui.html}, com o botão Authorize já configurado
 * para o Bearer usado por toda a API.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    OpenAPI fastrelaxOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FastRelax API")
                        .version("v1")
                        .description("Agendamento de sessões de descanso na janela de almoço dos colaboradores."))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
