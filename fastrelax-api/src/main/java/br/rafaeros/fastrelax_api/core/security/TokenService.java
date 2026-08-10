package br.rafaeros.fastrelax_api.core.security;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;

import br.rafaeros.fastrelax_api.features.users.User;

@Service
public class TokenService {

    @Value("${api.security.token.secret:my-secret-key}")
    private String secret;

    @Value("${api.security.token.expiration-hours:2}")
    private long accessTokenHours;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("fastrelax_api")
                    .withSubject(user.getEmail())
                    .withClaim("role", user.getRole().name())
                    .withClaim("userType", "USER")
                    .withExpiresAt(getExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String generateToken(br.rafaeros.fastrelax_api.features.collaborators.Collaborator collaborator) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("fastrelax_api")
                    .withSubject(String.valueOf(collaborator.getId()))
                    .withClaim("role", "COLLABORATOR")
                    .withClaim("userType", "COLLABORATOR")
                    .withExpiresAt(getExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public com.auth0.jwt.interfaces.DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("fastrelax_api")
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

    /**
     * Usa o fuso da JVM (fixado em ApplicationTimeZoneConfig) em vez de um offset
     * fixo: com -03:00 hardcoded, o token expirava uma hora antes ou depois
     * durante o horário de verão de outras regiões.
     */
    private Instant getExpirationDate() {
        return LocalDateTime.now().plusHours(accessTokenHours).atZone(ZoneId.systemDefault()).toInstant();
    }
}