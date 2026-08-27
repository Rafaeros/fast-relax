/**
 * Ajudantes de formulário compartilhados entre as features.
 */

/**
 * Diz se o estado do formulário traz erro em algum campo.
 *
 * As Server Actions devolvem `fieldErrors` como objeto sempre que a resposta da
 * API é mapeada — vazio quando nenhum erro coube num campo específico. Testar a
 * variável direto (`!state.fieldErrors`) dá falso para `{}`, e o recado geral do
 * servidor deixava de virar toast: o modal só piscava, sem explicar nada.
 */
export function hasFieldErrors(fieldErrors?: object): boolean {
  return Boolean(fieldErrors && Object.keys(fieldErrors).length > 0);
}
