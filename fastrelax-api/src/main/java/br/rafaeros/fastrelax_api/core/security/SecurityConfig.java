package br.rafaeros.fastrelax_api.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.rafaeros.fastrelax_api.core.tenancy.TenantContextFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final DeviceTokenFilter deviceTokenFilter;
    private final TenantContextFilter tenantContextFilter;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/collaborator/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                // Recuperação de senha: quem chega aqui perdeu o acesso, então
                // exigir autenticação seria contraditório. O limite de tentativas
                // por IP é o que segura o abuso.
                .requestMatchers("/auth/password/**").permitAll()
                // Documentação e healthcheck ficam abertos; as demais rotas do
                // actuator continuam exigindo autenticação.
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Autenticado pelo DeviceTokenFilter, não por JWT: o ESP32 não faz login.
                .requestMatchers(HttpMethod.POST, "/chairs/heartbeat").permitAll()
                // Cadastro de empresas e catálogo de firmware são da equipe da
                // plataforma. As demais rotas resolvem o papel no @PreAuthorize,
                // que é onde a regra fica junto do caso de uso.
                //
                // /companies/me é a exceção: RH/admin do cliente lendo a própria
                // empresa. Precisa vir antes do bloqueio geral — a primeira regra
                // que casar decide, e "/companies/**" casaria primeiro.
                .requestMatchers(HttpMethod.GET, "/companies/me").authenticated()
                .requestMatchers("/companies/**").hasRole("SYSADMIN")
                .requestMatchers(HttpMethod.POST, "/firmwares/**").hasRole("SYSADMIN")
                .requestMatchers(HttpMethod.PUT, "/firmwares/**").hasRole("SYSADMIN")
                .requestMatchers(HttpMethod.DELETE, "/firmwares/**").hasRole("SYSADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(deviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
            // Depois do SecurityFilter, que é quem coloca o principal no contexto:
            // é dele que sai a empresa da requisição.
            .addFilterAfter(tenantContextFilter, SecurityFilter.class)
            // Por último: precisa do usuário já autenticado para saber se ele ainda
            // está com a senha temporária.
            .addFilterAfter(passwordChangeRequiredFilter, TenantContextFilter.class)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
