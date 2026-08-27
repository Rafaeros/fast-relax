package br.rafaeros.fastrelax_api.core.mail;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entrega por SMTP.
 *
 * <p>
 * Só existe como bean quando {@code app.mail.enabled=true}; caso contrário quem
 * responde é o {@link LoggingMailSender}. Escolher na configuração, e não com um
 * {@code if} no meio do envio, é o que mantém esta classe com uma
 * responsabilidade só.
 *
 * <p>
 * Falha não sobe: o chamador precisa poder seguir com a senha temporária quando
 * o provedor está fora. A causa real vai para o log; a decisão fica com quem
 * chamou.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SmtpMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties properties;

    @Override
    public boolean send(MailMessage message) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            // multipart: o cliente escolhe entre HTML e texto puro.
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(properties.getFrom(), properties.getFromName());
            if (message.toName() == null || message.toName().isBlank()) {
                helper.setTo(message.to());
            } else {
                helper.setTo(new jakarta.mail.internet.InternetAddress(
                        message.to(), message.toName(), StandardCharsets.UTF_8.name()));
            }
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());

            javaMailSender.send(mimeMessage);
            return true;
        } catch (MailException | jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            // O endereço do destinatário não vai para o log: e-mail de colaborador
            // é dado pessoal, e log costuma sair da máquina.
            log.error("Falha ao enviar e-mail \"{}\": {}", message.subject(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isConfigured();
    }
}
