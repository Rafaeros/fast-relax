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
     * Uma resposta só para toda recusa <em>de credencial</em> do colaborador.
     *
     * <p>
     * Empresa inexistente, CPF que não está lá e senha errada dizem exatamente a
     * mesma coisa. Diferenciá-los seria entregar, de graça, quem é cliente da
     * Physical e quem trabalha em cada cliente.
     */
    private static final String INVALID_CREDENTIALS = "Empresa, CPF ou senha inválidos";

    /**
     * Recusas que só aparecem <b>depois</b> da senha conferida.
     *
     * <p>
     * Aqui a mensagem pode ser específica sem virar oráculo: quem chegou até este
     * ponto provou a senha, então já sabia que a conta existe. O que estas duas
     * frases acrescentam é o que a genérica escondia — não adianta tentar de novo,
     * nem trocar a senha; o caminho é falar com o RH.
     */
    private static final String COLLABORATOR_DISABLED = "Seu acesso está desativado. Fale com o RH da sua empresa para reativá-lo.";

    private static final String COMPANY_DISABLED = "O acesso da sua empresa está suspenso. Fale com o RH da sua empresa.";

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

        if (!credentialService.matches(collaborator, data.password())) {
            throw new BusinessException(INVALID_CREDENTIALS);
        }

        // Só depois da senha conferida: a ordem é o que separa "avisar quem tem
        // direito de saber" de "responder a quem está adivinhando CPF".
        requireActiveAccess(collaborator);

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
     * Por que o acesso foi recusado a quem já provou a senha.
     *
     * <p>
     * A empresa vem primeiro porque é a causa mais abrangente: com o contrato
     * suspenso, todo mundo daquele cliente esbarra aqui, e dizer "seu cadastro
     * está desativado" mandaria cada pessoa pedir ao RH uma reativação
     * individual que não resolveria nada.
     */
    private void requireActiveAccess(Collaborator collaborator) {
        Company company = collaborator.getCompany();
        if (company == null || !company.isEnabled()) {
            throw new BusinessException(COMPANY_DISABLED);
        }

        if (!collaborator.isEnabled()) {
            throw new BusinessException(COLLABORATOR_DISABLED);
        }
    }

    /**
     * Slug fora do formato e CPF malformado morrem aqui em silêncio, como um
     * cadastro que não existe. Deixá-los estourar produziria uma mensagem
     * diferente da de senha errada, e portanto um oráculo.
     *
     * <p>
     * A empresa desativada <em>não</em> é filtrada: descartá-la aqui devolveria a
     * mensagem genérica de credencial inválida e deixaria o colaborador tentando
     * a senha de novo, sem saber que o problema não é dele. Quem decide é
     * {@link #requireActiveAccess(Collaborator)}, depois da senha.
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
                .orElseThrow(() -> new BusinessException(COLLABORATOR_DISABLED));

        // Mesma distinção do login: quem apresenta um refresh token válido já
        // provou quem é, e merece saber se o que caiu foi o acesso dele ou o da
        // empresa inteira.
        requireActiveAccess(collaborator);
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
