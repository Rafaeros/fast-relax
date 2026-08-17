package br.rafaeros.fastrelax_api.features.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.security.LoginRateLimiter;
import br.rafaeros.fastrelax_api.core.security.TokenService;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorService;
import br.rafaeros.fastrelax_api.features.users.User;
import br.rafaeros.fastrelax_api.features.users.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final CollaboratorService collaboratorService;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    public LoginResponseDTO login(LoginRequestDTO data, String clientKey) {
        loginRateLimiter.checkAndRegister(clientKey);

        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        User user = (User) auth.getPrincipal();

        loginRateLimiter.reset(clientKey);
        return new LoginResponseDTO(
                tokenService.generateToken(user),
                refreshTokenService.issue(RefreshToken.SubjectType.USER, user.getId()),
                tokenService.getAccessTokenExpirationSeconds(),
                user.isMustChangePassword());
    }

    public CollaboratorLoginResponseDTO collaboratorLogin(CollaboratorLoginRequestDTO data, String clientKey) {
        loginRateLimiter.checkAndRegister(clientKey);

        // O blind index faz a busca ser a própria verificação de credencial:
        // só o CPF correto produz o digest armazenado.
        Collaborator collaborator = collaboratorService.findByCpf(data.cpf());

        loginRateLimiter.reset(clientKey);
        return new CollaboratorLoginResponseDTO(
                tokenService.generateToken(collaborator),
                refreshTokenService.issue(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId()),
                tokenService.getAccessTokenExpirationSeconds(),
                collaborator.getId(),
                collaborator.getName());
    }

    /**
     * Troca o refresh token por um par novo. O token apresentado é consumido, então
     * o cliente precisa guardar o que vem na resposta.
     */
    public LoginResponseDTO refresh(RefreshTokenRequestDTO data) {
        RefreshToken consumed = refreshTokenService.consume(data.refreshToken());

        if (consumed.getSubjectType() == RefreshToken.SubjectType.USER) {
            User user = userRepository.findById(consumed.getSubjectId())
                    .filter(candidate -> candidate.isEnabled())
                    .orElseThrow(() -> new BusinessException("Usuário indisponível. Faça login novamente."));
            return new LoginResponseDTO(
                    tokenService.generateToken(user),
                    refreshTokenService.issue(RefreshToken.SubjectType.USER, user.getId()),
                    tokenService.getAccessTokenExpirationSeconds(),
                    user.isMustChangePassword());
        }

        // Revalida o estado atual: quem foi desativado depois do login não renova.
        Collaborator collaborator = collaboratorRepository.findById(consumed.getSubjectId())
                .filter(candidate -> candidate.isEnabled())
                .orElseThrow(() -> new BusinessException(
                        "Seu acesso está desativado. Entre em contato com o RH."));
        // Colaborador não tem senha: o CPF é a credencial, então nunca há troca pendente.
        return new LoginResponseDTO(
                tokenService.generateToken(collaborator),
                refreshTokenService.issue(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId()),
                tokenService.getAccessTokenExpirationSeconds(),
                false);
    }

    /** Invalida apenas o dispositivo que fez logout. */
    public void logout(RefreshTokenRequestDTO data) {
        refreshTokenService.revoke(data.refreshToken());
    }
}
