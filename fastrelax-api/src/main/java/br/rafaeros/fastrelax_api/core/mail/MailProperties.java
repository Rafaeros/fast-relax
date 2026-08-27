package br.rafaeros.fastrelax_api.core.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuração do canal de e-mail (prefixo {@code app.mail}).
 *
 * <p>
 * Separado das propriedades {@code spring.mail.*}, que são do transporte (host,
 * porta, credenciais). Aqui fica o que é do produto: remetente exibido, se o
 * canal está ligado e qual endereço público montar nos links.
 *
 * <p>
 * {@code enabled} default {@code false}: sem isto, subir em desenvolvimento sem
 * SMTP faria todo cadastro tentar uma conexão que vai falhar, e esperar o
 * timeout dela.
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

    /** Liga o envio de verdade. Desligado, as mensagens só vão para o log. */
    private boolean enabled = false;

    /**
     * Remetente. No Gmail precisa ser a própria conta autenticada — o servidor
     * recusa um {@code From} de outro domínio.
     */
    private String from = "";

    /** Nome exibido ao lado do endereço na caixa de entrada. */
    private String fromName = "FastRelax";

    /**
     * Base pública do frontend, usada para montar os links de convite e de
     * recuperação.
     *
     * <p>
     * Vem da configuração e não do {@code Host} da requisição: o cabeçalho é
     * controlado por quem chama, e usá-lo permitiria forjar um link de
     * redefinição apontando para um domínio do atacante — com um token válido
     * dentro.
     */
    private String appBaseUrl = "http://localhost";

    public boolean isConfigured() {
        return enabled && !from.isBlank();
    }
}
