package br.rafaeros.fastrelax_api.core.dto;

import br.rafaeros.fastrelax_api.core.security.CredentialProvisioning;

/**
 * Como o acesso de uma conta recém-criada foi entregue.
 *
 * <p>
 * Um formato só para os três cadastros que criam credencial — usuário do
 * painel, colaborador e importação de planilha. A tela precisa da mesma
 * resposta nos três casos: mostro a senha para copiar, ou aviso que o convite
 * saiu?
 *
 * @param kind              {@code INVITE_SENT} ou {@code TEMPORARY_PASSWORD}
 * @param temporaryPassword em claro e apenas nesta resposta, porque o banco
 *                          guarda só o hash; nulo quando houve convite
 * @param email             para onde o convite foi; nulo quando houve senha
 */
public record CredentialDeliveryDTO(String kind, String temporaryPassword, String email) {

    public static CredentialDeliveryDTO from(CredentialProvisioning provisioning) {
        return new CredentialDeliveryDTO(
                provisioning.kind().name(),
                provisioning.temporaryPassword(),
                provisioning.email());
    }
}
