package br.rafaeros.fastrelax_api.core.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Bloqueia a API para quem ainda usa a senha temporária.
 *
 * <p>
 * Sem isto, {@code mustChangePassword} seria apenas uma sugestão que o cliente
 * poderia ignorar — a pessoa continuaria operando com a senha que outra escolheu
 * por ela.
 *
 * <p>
 * Vale para qualquer {@link CredentialHolder}: desde que o colaborador ganhou
 * senha própria, ele passou pela mesma exigência do painel, e o filtro não
 * precisou aprender um segundo tipo para isso.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    /** O mínimo para o dono da credencial conseguir sair dessa situação. */
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/users/me",
            "/users/me/first-access-password",
            "/collaborators/me",
            "/collaborators/me/first-access-password",
            "/auth/logout",
            "/auth/refresh");

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean blocked = authentication != null
                && authentication.getPrincipal() instanceof CredentialHolder holder
                && holder.isMustChangePassword()
                && !isAllowed(request);

        if (blocked) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"status":"warning",\
                    "message":"Defina sua senha antes de continuar.",\
                    "timestamp":"%s"}""".formatted(java.time.LocalDateTime.now()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** O path do servlet já vem sem o context-path, então basta comparar direto. */
    private boolean isAllowed(HttpServletRequest request) {
        String path = request.getServletPath();
        return ALLOWED_PATHS.contains(path) || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
