package br.rafaeros.fastrelax_api.features.auth;

import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.security.CredentialService;
import br.rafaeros.fastrelax_api.core.security.LoginRateLimiter;
import br.rafaeros.fastrelax_api.core.security.TokenService;
import br.rafaeros.fastrelax_api.core.util.CpfUtils;
import br.rafaeros.fastrelax_api.core.util.SlugUtils;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.companies.CompanyRepository;
import br.rafaeros.fastrelax_api.features.users.User;
import br.rafaeros.fastrelax_api.features.users.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Uma resposta só para toda recusa do login do colaborador.
     *
     * <p>
     * Empresa inexistente, CPF que não está lá, senha errada e cadastro
     * desativado dizem exatamente a mesma coisa. Diferenciá-los seria entregar,
     * de graça, quem é cliente da Physical e quem trabalha em cada cliente.
     */
    private static final String INVALID_CREDENTIALS = "Empresa, CPF ou senha inválidos";

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final CredentialService credentialService;
    private final CryptoService cryptoService;
    private final CollaboratorRepository collaboratorRepository;
    private final CompanyRepository companyRepository;
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

    /**
     * Login do colaborador: empresa pelo slug, pessoa pelo blind index do CPF,
     * credencial pela senha.
     *
     * <p>
     * Antes o blind index fazia os três papéis de uma vez — encontrar a pessoa
     * <em>era</em> autenticá-la. O acesso valia, na prática, o que vale um CPF,
     * que circula em qualquer cadastro; a senha é o que separa identificar de
     * provar identidade.
     */
    public CollaboratorLoginResponseDTO collaboratorLogin(CollaboratorLoginRequestDTO data, String clientKey) {
        loginRateLimiter.checkAndRegister(clientKey);

        Collaborator collaborator = findCandidate(data)
                .orElseThrow(() -> {
                    // Gasta o tempo de um bcrypt mesmo sem ter contra o que comparar:
                    // a resposta rápida denunciaria que o CPF não existe naquela empresa.
                    credentialService.wasteMatch(data.password());
                    return new BusinessException(INVALID_CREDENTIALS);
                });

        if (!credentialService.matches(collaborator, data.password()) || !collaborator.isEnabled()) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }

        loginRateLimiter.reset(clientKey);
        Company company = collaborator.getCompany();
        return new CollaboratorLoginResponseDTO(
                tokenService.generateToken(collaborator),
                refreshTokenService.issue(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId()),
                tokenService.getAccessTokenExpirationSeconds(),
                collaborator.getId(),
                collaborator.getName(),
                company.getId(),
                company.getName(),
                collaborator.isMustChangePassword());
    }

    /**
     * Slug fora do formato e CPF malformado morrem aqui em silêncio, como um
     * cadastro que não existe. Deixá-los estourar produziria uma mensagem
     * diferente da de senha errada, e portanto um oráculo.
     */
    private Optional<Collaborator> findCandidate(CollaboratorLoginRequestDTO data) {
        String slug = SlugUtils.sanitize(data.companySlug());
        if (!SlugUtils.isValid(slug)) {
            return Optional.empty();
        }
        String cpf = data.cpf() == null ? "" : data.cpf().replaceAll("\\D", "");
        if (!CpfUtils.hasValidCheckDigits(cpf)) {
            return Optional.empty();
        }

        return companyRepository.findBySlug(slug)
                .filter(Company::isEnabled)
                .flatMap(company -> collaboratorRepository
                        .findByCompanyIdAndCpfHash(company.getId(), cryptoService.blindIndex(cpf)));
    }

    /**
     * Troca o refresh token por um par novo. O token apresentado é consumido, então
     * o cliente precisa guardar o que vem na resposta.
     */
    public LoginResponseDTO refresh(RefreshTokenRequestDTO data) {
        RefreshToken consumed = refreshTokenService.consume(data.refreshToken());

        if (consumed.getSubjectType() == RefreshToken.SubjectType.USER) {
            User user = userRepository.findById(consumed.getSubjectId())
                    .filter(User::isEnabled)
                    .orElseThrow(() -> new BusinessException("Usuário indisponível. Faça login novamente."));
            return new LoginResponseDTO(
                    tokenService.generateToken(user),
                    refreshTokenService.issue(RefreshToken.SubjectType.USER, user.getId()),
                    tokenService.getAccessTokenExpirationSeconds(),
                    user.isMustChangePassword());
        }

        // Revalida o estado atual: quem foi desativado — ou cuja empresa foi
        // suspensa — depois do login não renova.
        Collaborator collaborator = collaboratorRepository.findById(consumed.getSubjectId())
                .filter(candidate -> candidate.isEnabled())
                .orElseThrow(() -> new BusinessException(
                        "Seu acesso está desativado. Entre em contato com o RH."));
        return new LoginResponseDTO(
                tokenService.generateToken(collaborator),
                refreshTokenService.issue(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId()),
                tokenService.getAccessTokenExpirationSeconds(),
                collaborator.isMustChangePassword());
    }

    /** Invalida apenas o dispositivo que fez logout. */
    public void logout(RefreshTokenRequestDTO data) {
        refreshTokenService.revoke(data.refreshToken());
    }
}
