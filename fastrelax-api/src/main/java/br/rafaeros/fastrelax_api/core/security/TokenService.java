package br.rafaeros.fastrelax_api.core.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.users.User;

/**
 * Emissão e verificação do token de acesso.
 *
 * <p>
 * O token carrega a empresa em {@code companyId}, mas ela é informativa: quem
 * decide o tenant é o {@code TenantContextFilter}, a partir do registro
 * recarregado do banco. Confiar no claim tornaria a troca de empresa uma questão
 * de editar um JWT — e o filtro de isolamento passaria a proteger o que o
 * atacante escolhesse.
 */
@Service
public class TokenService {

    private static final String ISSUER = "fastrelax_api";

    public static final String CLAIM_USER_TYPE = "userType";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_COMPANY_ID = "companyId";

    public static final String TYPE_USER = "USER";
    public static final String TYPE_COLLABORATOR = "COLLABORATOR";

    @Value("${api.security.token.secret:my-secret-key}")
    private String secret;

    @Value("${api.security.token.expiration-hours:2}")
    private long accessTokenHours;

    /** O subject do usuário do painel é o e-mail, que é único no sistema inteiro. */
    public String generateToken(User user) {
        return sign(builder -> builder
                .withSubject(user.getEmail())
                .withClaim(CLAIM_ROLE, user.getRole().name())
                .withClaim(CLAIM_USER_TYPE, TYPE_USER)
                .withClaim(CLAIM_COMPANY_ID, user.tenantCompanyId()));
    }

    /**
     * O subject do colaborador é o id: o CPF nunca trafega no token, e o blind
     * index só é único dentro da empresa.
     */
    public String generateToken(Collaborator collaborator) {
        return sign(builder -> builder
                .withSubject(String.valueOf(collaborator.getId()))
                .withClaim(CLAIM_ROLE, TYPE_COLLABORATOR)
                .withClaim(CLAIM_USER_TYPE, TYPE_COLLABORATOR)
                .withClaim(CLAIM_COMPANY_ID, collaborator.tenantCompanyId()));
    }

    public DecodedJWT validateToken(String token) {
        try {
            return JWT.require(algorithm())
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    /** Validade do token de acesso, para o cliente saber quando renovar. */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenHours * 3600L;
    }

    private String sign(java.util.function.UnaryOperator<JWTCreator.Builder> claims) {
        try {
            return claims.apply(JWT.create().withIssuer(ISSUER))
                    .withExpiresAt(expiration())
                    .sign(algorithm());
        } catch (JWTCreationException exception) {
            throw new IllegalStateException("Erro ao gerar token JWT", exception);
        }
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(secret);
    }

    /**
     * Usa o fuso da JVM (fixado em {@code ApplicationTimeZoneConfig}) em vez de um
     * offset fixo: com -03:00 hardcoded, o token expirava uma hora antes ou depois
     * durante o horário de verão de outras regiões.
     */
    private Instant expiration() {
        return LocalDateTime.now().plusHours(accessTokenHours).atZone(ZoneId.systemDefault()).toInstant();
    }
}
