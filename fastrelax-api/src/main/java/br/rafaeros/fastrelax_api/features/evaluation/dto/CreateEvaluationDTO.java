package br.rafaeros.fastrelax_api.features.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O que o app envia ao fechar o modal de avaliação.
 *
 * <p>
 * Sem {@code collaboratorId}: quem avalia é sempre o colaborador logado, e a
 * sessão já diz de quem ela é. Aceitar o id pelo corpo abriria espaço para
 * avaliar no lugar de outra pessoa e só seria recusado por uma checagem a mais
 * no serviço — mais simples não oferecer o campo.
 *
 * @param comments opcional: a nota é obrigatória, o comentário não. Exigir texto
 *                 faria a maioria mandar qualquer coisa para fechar o modal.
 */
public record CreateEvaluationDTO(

        @NotNull(message = "Informe a sessão avaliada") Long sessionId,

        @NotNull(message = "Precisa ser um número entre 1 e 5") @Min(value = 1, message = "Precisa ser um número entre 1 e 5") @Max(value = 5, message = "Precisa ser um número entre 1 e 5") Integer score,

        @Size(max = 500, message = "O comentário pode ter no máximo 500 caracteres") String comments

) {
}
