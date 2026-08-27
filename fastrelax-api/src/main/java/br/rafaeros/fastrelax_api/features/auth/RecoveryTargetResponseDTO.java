package br.rafaeros.fastrelax_api.features.auth;

/**
 * O que a tela de redefinição mostra ao abrir o link.
 *
 * <p>
 * Só o necessário para saudar a pessoa e escolher o texto: e-mail e papel ficam
 * de fora porque a URL pode ter sido aberta por quem não é o dono — um link
 * encaminhado, um histórico compartilhado.
 *
 * @param name     nome de quem recebeu o link
 * @param purpose  INVITE (conta nova) ou RESET (recuperação)
 * @param audience USER ou COLLABORATOR, para a tela saber a qual login voltar
 */
public record RecoveryTargetResponseDTO(
    String name,
    String purpose,
    String audience
) {
    public static RecoveryTargetResponseDTO from(PasswordRecoveryService.RecoveryTarget target) {
        return new RecoveryTargetResponseDTO(
                target.name(),
                target.purpose().name(),
                target.subjectType().name());
    }
}
