package br.rafaeros.fastrelax_api.features.evaluation;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedEntity;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A nota que o colaborador dá para a massagem que acabou de fazer.
 *
 * <p>
 * Não tem soft delete nem edição: avaliação é o que a pessoa sentiu naquele
 * momento, e reescrevê-la depois transformaria o histórico do RH em algo que
 * não dá para acompanhar ao longo do tempo.
 *
 * <p>
 * Herda de {@link CompanyScopedEntity} como todo o resto: sem {@code company_id}
 * próprio, a listagem do RH dependeria de join com a sessão para se manter
 * dentro do tenant — exatamente o tipo de filtro que uma consulta nova esquece.
 */
@Entity
@Table(name = "session_evaluations")
@Getter
@Setter
@NoArgsConstructor
public class Evaluation extends CompanyScopedEntity {

    /**
     * Quem avaliou. Redundante em relação a {@code session.collaborator}, e
     * mantido porque é por ele que o RH agrupa — sem isso, todo relatório por
     * pessoa passaria por join com a sessão.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collaborator_id", nullable = false)
    private Collaborator collaborator;

    /**
     * Sessão avaliada. {@code unique}: uma massagem recebe uma nota só, e quem
     * garante isso de fato é a constraint do banco — a checagem da aplicação roda
     * antes do commit e dois envios simultâneos passariam pelos dois.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private CollaboratorSession session;

    /** 1 a 5, na escala de {@link EvaluationScore}. */
    @Min(1)
    @Max(5)
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "comments", length = 500)
    private String comments;

    @CreationTimestamp
    @Column(name = "evaluation_date", nullable = false, updatable = false)
    private LocalDateTime evaluationDate;
}
