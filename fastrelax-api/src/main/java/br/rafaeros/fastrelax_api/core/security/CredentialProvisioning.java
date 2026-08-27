package br.rafaeros.fastrelax_api.core.security;

/**
 * Como a conta recém-criada vai receber acesso.
 *
 * <p>
 * Existe para o cadastro responder a pergunta que a tela precisa fazer: mostro
 * uma senha para o RH copiar, ou aviso que o convite saiu? Sem isso, cada
 * chamador teria de reproduzir a mesma decisão a partir de "o e-mail está
 * preenchido?" e "o SMTP respondeu?" — e a interface acabaria mentindo em um
 * dos casos.
 *
 * @param kind              caminho efetivamente tomado
 * @param temporaryPassword em claro, só em {@link Kind#TEMPORARY_PASSWORD}; é a
 *                          única vez que existe fora do cliente, porque o banco
 *                          guarda apenas o hash
 * @param email             destinatário do convite, só em {@link Kind#INVITE_SENT}
 */
public record CredentialProvisioning(Kind kind, String temporaryPassword, String email) {

    public enum Kind {
        /** Link de definição de senha enviado; nenhuma senha foi gerada. */
        INVITE_SENT,
        /** Senha temporária gerada, para quem cadastrou repassar. */
        TEMPORARY_PASSWORD
    }

    public static CredentialProvisioning inviteSent(String email) {
        return new CredentialProvisioning(Kind.INVITE_SENT, null, email);
    }

    public static CredentialProvisioning temporaryPassword(String password) {
        return new CredentialProvisioning(Kind.TEMPORARY_PASSWORD, password, null);
    }

    public boolean isInvite() {
        return kind == Kind.INVITE_SENT;
    }
}
