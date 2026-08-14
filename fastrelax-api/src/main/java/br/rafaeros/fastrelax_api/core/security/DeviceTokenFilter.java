package br.rafaeros.fastrelax_api.core.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica o ESP32 nas rotas de dispositivo.
 *
 * <p>
 * O hardware não faz login nem carrega JWT: usa um segredo compartilhado gravado
 * no firmware. Sem isso, qualquer máquina da rede poderia forjar heartbeats e
 * fazer o sistema acreditar que uma cadeira offline está disponível.
 */
@Component
public class DeviceTokenFilter extends OncePerRequestFilter {

    private static final String DEVICE_PATH = "/chairs/heartbeat";
    private static final String HEADER = "X-Device-Token";

    @Value("${app.chair.device-token:}")
    private String deviceToken;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        if (!DEVICE_PATH.equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (deviceToken.isBlank() || !deviceToken.equals(provided)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"status":"warning","message":"Dispositivo não autorizado.","timestamp":"%s"}"""
                    .formatted(java.time.LocalDateTime.now()));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
