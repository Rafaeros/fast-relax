package br.rafaeros.fastrelax_api.core.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final CollaboratorRepository collaboratorRepository;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, 
                                    @org.springframework.lang.NonNull HttpServletResponse response, 
                                    @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        // 1. Tenta extrair o token do cabeçalho da requisição
        var token = this.recoverToken(request);

        if (token != null) {
            // 2. Valida o token e extrai os dados
            var decodedJWT = tokenService.validateToken(token);

            if (decodedJWT != null) {
                String subject = decodedJWT.getSubject();
                String userType = decodedJWT.getClaim("userType").asString();

                UserDetails user = null;
                if ("COLLABORATOR".equals(userType)) {
                    // O subject do token do colaborador é o id; o CPF nunca trafega no JWT.
                    user = parseCollaboratorId(subject)
                            .flatMap(collaboratorRepository::findById)
                            .orElse(null);
                } else {
                    // 3. Busca o usuário no banco de dados
                    user = userRepository.findByEmail(subject).orElse(null);
                }

                // 4. Cria o objeto de autenticação do Spring e insere no contexto da requisição.
                // isEnabled() cobre active e deletedAt: sem esta checagem, um token
                // emitido antes da desativação continuaria valendo até expirar.
                if (user != null && user.isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // 5. Continua o fluxo da requisição (passa para o próximo filtro ou para o
        // Controller)
        filterChain.doFilter(request, response);
    }

    private Optional<Long> parseCollaboratorId(String subject) {
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException e) {
            // Token antigo, emitido quando o subject era o hash do CPF.
            return Optional.empty();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        // Remove o prefixo "Bearer " para sobrar apenas o hash do JWT
        return authHeader.replace("Bearer ", "");
    }
}