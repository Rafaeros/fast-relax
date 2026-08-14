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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final DeviceTokenFilter deviceTokenFilter;
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
                // Documentação e healthcheck ficam abertos; as demais rotas do
                // actuator continuam exigindo autenticação.
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Autenticado pelo DeviceTokenFilter, não por JWT: o ESP32 não faz login.
                .requestMatchers(HttpMethod.POST, "/chairs/heartbeat").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(deviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
            // Depois do SecurityFilter: precisa do usuário já autenticado no contexto
            // para saber se ele ainda está com a senha temporária.
            .addFilterAfter(passwordChangeRequiredFilter, SecurityFilter.class)
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
