package br.rafaeros.fastrelax_api.core.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Canal de e-mail desligado.
 *
 * <p>
 * Condição espelhada à do {@link SmtpMailSender}, e não
 * {@code @ConditionalOnMissingBean}: em classe anotada com {@code @Component} a
 * ausência é avaliada durante a varredura, cuja ordem não é garantida — daria
 * dois beans ou nenhum, dependendo do dia. Existir um bean inerte, em vez de
 * nenhum, evita {@code Optional<MailSender>} e checagem de nulo em cada
 * chamador.
 *
 * <p>
 * {@link #isEnabled()} responde {@code false}, e é isso que faz o cadastro cair
 * no fluxo de senha temporária: sem canal configurado, mandar convite seria
 * criar uma conta que ninguém consegue ativar.
 *
 * <p>
 * O corpo vai para o log em nível DEBUG para o desenvolvimento conseguir seguir
 * o link do convite sem SMTP. Em produção o canal está ligado, então isto não
 * roda — mas o log fica em DEBUG de qualquer forma: o corpo carrega o token.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingMailSender implements MailSender {

    @Override
    public boolean send(MailMessage message) {
        log.info("E-mail não enviado (canal desligado): \"{}\"", message.subject());
        log.debug("Corpo da mensagem não enviada:\n{}", message.textBody());
        // Falso de propósito: quem chama precisa saber que ninguém recebeu nada.
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
