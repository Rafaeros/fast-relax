package br.rafaeros.fastrelax_api.core.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.mail.CredentialMailTemplates;
import br.rafaeros.fastrelax_api.core.mail.MailSender;
import br.rafaeros.fastrelax_api.features.auth.CredentialToken;
import br.rafaeros.fastrelax_api.features.auth.CredentialTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dá acesso a uma conta recém-criada.
 *
 * <p>
 * Um lugar só decide entre os dois caminhos, e os dois cadastros — usuário do
 * painel e colaborador — chamam este mesmo método. Duplicar a decisão faria os
 * fluxos divergirem: já aconteceria hoje, porque o colaborador pode não ter
 * e-mail e o usuário sempre tem.
 *
 * <p>
 * <b>Convite</b> quando há e-mail e canal configurado: nenhuma senha é gerada, e
 * a pessoa define a própria pelo link. É melhor que a temporária porque nada
 * secreto passa por WhatsApp nem fica anotado num papel na mesa do RH.
 *
 * <p>
 * <b>Senha temporária</b> quando não há e-mail — ou quando o envio falha. Sem
 * esse desvio, um SMTP fora do ar impediria o RH de cadastrar qualquer pessoa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialProvisioningService {

    private final CredentialService credentialService;
    private final CredentialTokenService tokenService;
    private final CredentialAccounts accounts;
    private final MailSender mailSender;
    private final CredentialMailTemplates templates;

    /**
     * Senha inutilizável para a conta que ainda não foi provisionada.
     *
     * <p>
     * A coluna é {@code NOT NULL} e o {@code UserDetails} do Spring não aceita
     * senha nula, então a conta convidada precisa de <em>alguma</em> coisa ali. O
     * valor é sorteado e descartado na hora: ninguém — nem quem cadastrou —
     * conhece a senha, e o login simplesmente não confere até o convite ser
     * aceito.
     */
    public void initialize(CredentialHolder holder) {
        credentialService.initializeWithUnusablePassword(holder);
    }

    /**
     * Entrega o acesso. A conta já precisa estar salva: o token de convite
     * referencia o id dela.
     *
     * <p>
     * Persiste o holder quando gera senha temporária — quem chama não precisa
     * lembrar de um segundo {@code save} que só existe em um dos caminhos.
     */
    @Transactional
    public CredentialProvisioning provision(CredentialHolder holder) {
        if (holder.hasEmail() && mailSender.isEnabled()) {
            String token = tokenService.issue(holder, CredentialToken.Purpose.INVITE);
            long hours = tokenService.validityHours(CredentialToken.Purpose.INVITE);

            boolean sent = mailSender.send(
                    templates.invite(holder.getEmail(), holder.getName(), token, hours));

            if (sent) {
                return CredentialProvisioning.inviteSent(holder.getEmail());
            }

            // O envio falhou depois de o token existir: derruba o link órfão para
            // ele não ficar válido por 48h sem ninguém saber que existe.
            tokenService.invalidatePending(holder);
            log.warn("Convite não enviado; caindo para senha temporária (conta {} {})",
                    holder.subjectType(), holder.getId());
        }

        return temporaryPasswordFor(holder);
    }

    private CredentialProvisioning temporaryPasswordFor(CredentialHolder holder) {
        String temporary = credentialService.initializeWithTemporaryPassword(holder);
        accounts.save(holder);
        return CredentialProvisioning.temporaryPassword(temporary);
    }
}
