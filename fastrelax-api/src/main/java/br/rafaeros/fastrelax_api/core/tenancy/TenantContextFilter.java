package br.rafaeros.fastrelax_api.core.tenancy;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Publica no {@link TenantContext} a empresa de quem fez a requisição.
 *
 * <p>
 * Roda depois do {@code SecurityFilter}, que é quem coloca o principal no
 * contexto de segurança. Requisição sem autenticação não define tenant nenhum —
 * e é assim que deve ser: qualquer consulta escopada falha em vez de assumir uma
 * empresa arbitrária.
 *
 * <p>
 * A limpeza no {@code finally} não é zelo: as threads vêm de um pool, e uma
 * identidade deixada para trás seria lida pela próxima requisição — de outro
 * cliente — como se fosse dela.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        resolveIdentity().ifPresent(TenantContext::set);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private java.util.Optional<TenantIdentity> resolveIdentity() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof TenantPrincipal principal)) {
            return java.util.Optional.empty();
        }
        Long companyId = principal.tenantCompanyId();
        return java.util.Optional.of(
                companyId == null ? TenantIdentity.platform() : TenantIdentity.ofCompany(companyId));
    }
}
