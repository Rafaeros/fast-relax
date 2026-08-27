package br.rafaeros.fastrelax_api.core.mail;

/**
 * Entrega de e-mail.
 *
 * <p>
 * Uma responsabilidade só: pegar uma {@link MailMessage} pronta e colocá-la na
 * rede. Não decide o que escrever, não decide quando enviar.
 *
 * <p>
 * O retorno é um booleano em vez de uma exceção porque quem chama precisa
 * <em>reagir</em> à falha, não abortar: se o convite não sai, o cadastro cai
 * para a senha temporária na tela. Tratar indisponibilidade de SMTP como erro
 * fatal deixaria o RH sem conseguir cadastrar ninguém enquanto o provedor
 * estivesse fora.
 */
public interface MailSender {

    /**
     * @return {@code true} quando a mensagem foi aceita pelo servidor de e-mail
     */
    boolean send(MailMessage message);

    /**
     * Se há um canal de e-mail configurado.
     *
     * <p>
     * Permite decidir o fluxo <em>antes</em> de criar a conta — convite ou senha
     * temporária —, em vez de descobrir pela falha e ter de desfazer.
     */
    boolean isEnabled();
}
