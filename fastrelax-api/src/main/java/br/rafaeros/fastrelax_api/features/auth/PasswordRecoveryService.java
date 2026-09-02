package br.rafaeros.fastrelax_api.features.auth;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.mail.CredentialMailTemplates;
import br.rafaeros.fastrelax_api.core.mail.MailSender;
import br.rafaeros.fastrelax_api.core.security.CredentialAccounts;
import br.rafaeros.fastrelax_api.core.security.CredentialHolder;
import br.rafaeros.fastrelax_api.core.security.CredentialService;
import br.rafaeros.fastrelax_api.core.util.SlugUtils;
import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.companies.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * "Esqueci minha senha" e a definição por link.
 *
 * <p>
 * Serve painel e colaborador com o mesmo código: o que difere é só como se
 * encontra a conta — e-mail global no painel, slug da empresa mais e-mail no
 * colaborador —, e isso está encapsulado no {@code CredentialAccount} de cada
 * tipo.
 *
 * <p>
 * <b>A resposta do pedido nunca varia.</b> E-mail inexistente, conta desativada,
 * empresa suspensa e envio bem-sucedido produzem a mesma mensagem. Distinguir
 * qualquer um deles transformaria a tela pública de recuperação num verificador
 * de "esta pessoa trabalha aqui?" — e, com o slug, num de "esta empresa é
 * cliente da Physical?".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    /**
     * Resposta única de {@code /password/forgot}, qualquer que seja o desfecho.
     */
    public static final String GENERIC_REQUEST_MESSAGE =
            "Se houver uma conta com este e-mail, o link de redefinição chegará em instantes.";

    private final CredentialTokenService tokenService;
    private final CredentialAccounts accounts;
    private final CredentialService credentialService;
    private final CompanyRepository companyRepository;
    private final MailSender mailSender;
    private final CredentialMailTemplates templates;

    /** Recuperação do painel: o e-mail é único no sistema inteiro. */
    @Transactional
    public void requestForUser(String email) {
        accounts.of(RefreshToken.SubjectType.USER)
                .flatMap(account -> account.findByEmail(normalize(email), null))
                .ifPresent(this::sendReset);
    }

    /**
     * Recuperação do colaborador: o e-mail só é único dentro da empresa, então o
     * slug é obrigatório para a busca não ser ambígua. É o mesmo slug que ele
     * digita no login, então não há informação nova a pedir.
     */
    @Transactional
    public void requestForCollaborator(String companySlug, String email) {
        String slug = SlugUtils.sanitize(companySlug);
        if (!SlugUtils.isValid(slug)) {
            // Some em silêncio: slug fora do formato é tentativa inválida, e
            // responder diferente já diria que o formato importa — e portanto
            // que existe uma busca acontecendo do outro lado.
            return;
        }

        companyRepository.findBySlug(slug)
                .filter(Company::isEnabled)
                .flatMap(company -> accounts.of(RefreshToken.SubjectType.COLLABORATOR)
                        .flatMap(account -> account.findByEmail(normalize(email), company.getId())))
                .ifPresent(this::sendReset);
    }

    /**
     * A quem pertence um link, sem gastá-lo.
     *
     * <p>
     * A tela usa para saudar pela primeira palavra do nome e escolher o texto
     * certo — "defina sua senha" num convite, "redefina" numa recuperação.
     * Devolve só isso: e-mail e papel não aparecem, porque a URL pode ter sido
     * aberta por quem não é o dono.
     */
    @Transactional(readOnly = true)
    public Optional<RecoveryTarget> describe(String token) {
        return tokenService.peek(token)
                .flatMap(stored -> accounts.find(stored.getSubjectType(), stored.getSubjectId())
                        .map(holder -> new RecoveryTarget(
                                holder.getName(),
                                stored.getPurpose(),
                                stored.getSubjectType())));
    }

    /**
     * Define a senha a partir do link e libera a conta.
     *
     * <p>
     * Serve para os dois tipos de token: convite e recuperação chegam ao mesmo
     * ponto — a pessoa provou controlar a caixa de entrada e escolheu uma senha.
     * O {@code mustChangePassword} cai junto, senão ela seria mandada de volta à
     * tela de primeiro acesso logo após definir a senha.
     */
    @Transactional
    public void completeWithToken(String token, String newPassword, String confirmation) {
        CredentialToken consumed = tokenService.consume(token)
                .orElseThrow(() -> new BusinessException(
                        "Link inválido ou expirado. Peça um novo para redefinir sua senha."));

        CredentialHolder holder = accounts.find(consumed.getSubjectType(), consumed.getSubjectId())
                .orElseThrow(() -> new BusinessException(
                        "A conta deste link não está mais disponível."));

        // Passa pelo caminho normal de troca: confirmação conferida, senha
        // diferente da atual, sessões e demais links derrubados.
        credentialService.changePasswordWithoutCurrent(holder, newPassword, confirmation);
        holder.setMustChangePassword(false);
        accounts.save(holder);
    }

    /**
     * Emite o token e manda o e-mail. Falha de envio não vira erro para quem
     * pediu: a resposta é genérica de qualquer forma, e insistir na tela não
     * consertaria um SMTP fora do ar.
     */
    private void sendReset(CredentialHolder holder) {
        if (!holder.hasEmail() || !mailSender.isEnabled()) {
            return;
        }

        String token = tokenService.issue(holder, CredentialToken.Purpose.RESET);
        long hours = tokenService.validityHours(CredentialToken.Purpose.RESET);

        boolean sent = mailSender.send(
                templates.passwordReset(holder.getEmail(), holder.getName(), token, hours));

        if (!sent) {
            // Link órfão não pode ficar de pé: ninguém sabe que ele existe.
            tokenService.invalidatePending(holder);
            log.warn("Recuperação não enviada para a conta {} {}",
                    holder.subjectType(), holder.getId());
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * O mínimo que a tela de redefinição precisa saber sobre o link.
     *
     * @param name        primeiro nome é o que a tela exibe; o resto vem junto
     *                    porque cortar é trabalho de apresentação
     * @param purpose     decide entre "defina" e "redefina"
     * @param subjectType para onde mandar depois de concluir — painel ou app
     */
    public record RecoveryTarget(
            String name,
            CredentialToken.Purpose purpose,
            RefreshToken.SubjectType subjectType) {
    }
}
