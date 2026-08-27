package br.rafaeros.fastrelax_api.core.mail;

import java.util.Objects;

/**
 * Um e-mail pronto para sair.
 *
 * <p>
 * Não conhece SMTP nem template: é o contrato entre quem escreve a mensagem
 * ({@code CredentialMailTemplates}) e quem a entrega ({@link MailSender}). Essa
 * separação é o que permite trocar o provedor — ou desligá-lo em
 * desenvolvimento — sem tocar em uma linha de texto.
 *
 * <p>
 * Os dois corpos viajam juntos de propósito: cliente que não renderiza HTML cai
 * no texto puro em vez de mostrar as tags cruas.
 *
 * @param to       destinatário
 * @param toName   nome de quem recebe, para o cabeçalho
 * @param subject  assunto
 * @param htmlBody corpo em HTML
 * @param textBody o mesmo conteúdo em texto puro
 */
public record MailMessage(
        String to,
        String toName,
        String subject,
        String htmlBody,
        String textBody) {

    public MailMessage {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(htmlBody, "htmlBody");
        Objects.requireNonNull(textBody, "textBody");
    }
}
