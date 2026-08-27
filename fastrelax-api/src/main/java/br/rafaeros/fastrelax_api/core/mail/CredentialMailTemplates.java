package br.rafaeros.fastrelax_api.core.mail;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * O texto dos e-mails de credencial.
 *
 * <p>
 * Uma responsabilidade: transformar (destinatário, token, validade) em
 * {@link MailMessage}. Não sabe enviar, não sabe emitir token. Mudar a redação,
 * o layout ou a assinatura acontece só aqui.
 *
 * <p>
 * Convite e recuperação compartilham o mesmo esqueleto porque são o mesmo
 * gesto do ponto de vista de quem recebe — "clique aqui e escolha sua senha".
 * O que muda é a explicação de <em>por que</em> o e-mail chegou, e é só isso
 * que cada método monta.
 */
@Component
@RequiredArgsConstructor
public class CredentialMailTemplates {

    /** Rota pública do frontend que consome o token. */
    private static final String RESET_PATH = "/redefinir-senha";

    private final MailProperties properties;

    /** Conta recém-criada: a pessoa ainda não tem senha nenhuma. */
    public MailMessage invite(String to, String name, String token, long validHours) {
        String link = link(token);
        String greeting = "Olá, " + firstName(name) + "!";
        String reason = "Uma conta foi criada para você no FastRelax. "
                + "Para começar a usar, defina sua senha:";

        return new MailMessage(
                to,
                name,
                "Seu acesso ao FastRelax",
                html(greeting, reason, link, "Definir minha senha", validHours),
                text(greeting, reason, link, validHours));
    }

    /** Recuperação pedida pela própria pessoa. */
    public MailMessage passwordReset(String to, String name, String token, long validHours) {
        String link = link(token);
        String greeting = "Olá, " + firstName(name) + "!";
        String reason = "Recebemos um pedido para redefinir sua senha do FastRelax. "
                + "Se foi você, escolha uma nova senha:";

        return new MailMessage(
                to,
                name,
                "Redefinição de senha do FastRelax",
                html(greeting, reason, link, "Redefinir minha senha", validHours)
                        + IGNORE_HTML,
                text(greeting, reason, link, validHours) + IGNORE_TEXT);
    }

    /**
     * O aviso de "ignore este e-mail" só aparece na recuperação.
     *
     * <p>
     * No convite ele seria enganoso: ignorar não desfaz nada, a conta continua
     * criada e esperando. Aqui é verdade — sem clicar, a senha atual segue
     * valendo.
     */
    private static final String IGNORE_HTML =
            "<p style=\"color:#6b7280;font-size:13px\">Se você não pediu isso, ignore este e-mail. "
                    + "Sua senha atual continua valendo.</p>";

    private static final String IGNORE_TEXT =
            "\n\nSe você não pediu isso, ignore este e-mail. Sua senha atual continua valendo.";

    /**
     * O token vai na query string, e não no caminho, para não ser tratado como
     * recurso por proxies e caches intermediários.
     */
    private String link(String token) {
        String base = properties.getAppBaseUrl().replaceAll("/+$", "");
        return base + RESET_PATH + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * HTML deliberadamente simples: tabela nenhuma, CSS inline e nada de imagem.
     * Cliente de e-mail corporativo costuma bloquear recurso externo, e o layout
     * elaborado chegaria quebrado.
     */
    private String html(String greeting, String reason, String link, String action, long validHours) {
        return """
                <div style="font-family:system-ui,-apple-system,Segoe UI,sans-serif;\
                max-width:520px;color:#111827;line-height:1.6">
                  <p style="font-size:16px;font-weight:600">%s</p>
                  <p>%s</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#b45309;color:#fff;text-decoration:none;\
                padding:12px 20px;border-radius:8px;display:inline-block;font-weight:600">%s</a>
                  </p>
                  <p style="color:#6b7280;font-size:13px">O link vale por %d horas e só pode ser \
                usado uma vez.</p>
                  <p style="color:#6b7280;font-size:13px">Se o botão não funcionar, copie este \
                endereço no navegador:<br><span style="word-break:break-all">%s</span></p>
                </div>"""
                .formatted(greeting, reason, link, action, validHours, link);
    }

    private String text(String greeting, String reason, String link, long validHours) {
        return """
                %s

                %s

                %s

                O link vale por %d horas e só pode ser usado uma vez."""
                .formatted(greeting, reason, link, validHours);
    }

    /** Trata pelo primeiro nome: o e-mail é pessoal, não um comunicado. */
    private String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "tudo bem";
        }
        return name.trim().split("\\s+")[0];
    }
}
