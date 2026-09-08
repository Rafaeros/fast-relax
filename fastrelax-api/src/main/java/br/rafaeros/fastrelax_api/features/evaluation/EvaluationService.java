package br.rafaeros.fastrelax_api.features.evaluation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSession;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSessionRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorSessionSpecifications;
import br.rafaeros.fastrelax_api.features.collaborators.SessionStatus;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionResponseDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.CreateEvaluationDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationFilterDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationResponseDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationSummaryDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationSummaryDTO.ScoreCountDTO;
import lombok.RequiredArgsConstructor;

/**
 * Avaliação da massagem: o colaborador dá a nota, o RH lê o resultado.
 *
 * <p>
 * Toda leitura passa pelos métodos {@code ...Scoped} do repositório, então uma
 * avaliação nunca atravessa a fronteira da empresa. Dentro da empresa a divisão
 * é por papel: RH e gestor veem tudo, colaborador só o que ele mesmo respondeu.
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    /**
     * Janela em que uma massagem concluída ainda pede nota.
     *
     * <p>
     * Sem esse teto, quem fechou o modal sem responder receberia o mesmo pedido
     * toda vez que abrisse o app, por tempo indeterminado — e a nota deixaria de
     * ser sobre a massagem para virar sobre a lembrança dela.
     */
    private static final int PENDING_WINDOW_HOURS = 24;

    private final EvaluationRepository evaluationRepository;
    private final CollaboratorSessionRepository sessionRepository;

    /**
     * Registra a nota da própria massagem.
     *
     * <p>
     * O colaborador vem do token, nunca do corpo: a sessão já diz de quem ela é, e
     * aceitar o id pelo request só criaria uma checagem a mais para impedir avaliar
     * no lugar de outra pessoa.
     */
    @Transactional
    public EvaluationResponseDTO create(CreateEvaluationDTO dto) {
        Collaborator me = Principals.requireCollaborator();

        // Escopado: sessão de outra empresa responde 404, não 403 — quem tentou não
        // fica sabendo que o id existe.
        CollaboratorSession session = sessionRepository.findByIdScoped(Objects.requireNonNull(dto.sessionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));

        if (!session.getCollaborator().getId().equals(me.getId())) {
            throw new AccessDeniedException("Acesso negado. Você só pode avaliar as suas próprias massagens.");
        }

        if (session.getStatus() != SessionStatus.DONE) {
            throw new BusinessException("Só é possível avaliar uma massagem depois que ela termina.");
        }

        if (evaluationRepository.existsBySessionId(session.getId())) {
            throw new BusinessException("Esta massagem já foi avaliada.");
        }

        Evaluation evaluation = new Evaluation();
        // A empresa vem da sessão, não do contexto: assim a avaliação nunca acaba em
        // um tenant diferente do da massagem que ela descreve.
        evaluation.setCompany(session.getCompany());
        evaluation.setCollaborator(session.getCollaborator());
        evaluation.setSession(session);
        evaluation.setScore(dto.score());
        evaluation.setComments(normalizeComments(dto.comments()));

        try {
            return new EvaluationResponseDTO(evaluationRepository.save(evaluation));
        } catch (DataIntegrityViolationException ex) {
            // Dois envios simultâneos passam os dois pela checagem acima; quem garante
            // uma nota por massagem é o índice único da coluna session_id.
            throw new BusinessException("Esta massagem já foi avaliada.");
        }
    }

    /**
     * Listagem paginada. RH e gestor veem a empresa inteira; colaborador só as
     * próprias respostas, mesmo que mande outro {@code collaboratorId} no filtro.
     */
    public Page<EvaluationResponseDTO> findAll(EvaluationFilterDTO filter, Pageable pageable) {
        return evaluationRepository.findAllScoped(toSpecification(filter), Objects.requireNonNull(pageable))
                .map(EvaluationResponseDTO::new);
    }

    /**
     * Números do topo da tela do RH, no mesmo recorte da listagem.
     *
     * <p>
     * A média sai da distribuição em vez de uma agregação própria: são as mesmas
     * cinco contagens que a tela já precisa para o gráfico, e uma consulta a menos
     * é uma chance a menos de os dois números discordarem entre si.
     */
    public EvaluationSummaryDTO summary(EvaluationFilterDTO filter) {
        Specification<Evaluation> spec = toSpecification(filter);

        List<ScoreCountDTO> distribution = new ArrayList<>();
        long total = 0;
        long weighted = 0;

        for (EvaluationScore score : EvaluationScore.values()) {
            long count = evaluationRepository.countScoped(
                    Specification.allOf(spec, EvaluationSpecifications.hasScore(score.getValue())));

            distribution.add(new ScoreCountDTO(score.getValue(), score.getLabel(), count));
            total += count;
            weighted += count * score.getValue();
        }

        // Base da adesão: massagens que chegaram ao fim no mesmo período. Sem ela,
        // "18 avaliações" não diz se são muitas ou poucas.
        long sessionsDone = sessionRepository.countScoped(Specification.allOf(
                CollaboratorSessionSpecifications.hasStatus(SessionStatus.DONE),
                CollaboratorSessionSpecifications.hasCollaborator(filter != null ? filter.collaboratorId() : null),
                CollaboratorSessionSpecifications.betweenDates(filter != null ? filter.from() : null,
                        filter != null ? filter.to() : null)));

        return new EvaluationSummaryDTO(
                total,
                total == 0 ? null : round((double) weighted / total),
                sessionsDone,
                sessionsDone == 0 ? null : round(total * 100.0 / sessionsDone),
                distribution);
    }

    /**
     * Massagem concluída que ainda espera nota, se houver.
     *
     * <p>
     * É o que permite o app reabrir o modal quando a pessoa fecha sem responder,
     * ou quando a sessão termina sozinha no fim do horário e ela volta ao app
     * depois. Vazio é resposta normal, não erro.
     */
    public Optional<CollaboratorSessionResponseDTO> findMyPendingSession() {
        Long collaboratorId = Principals.requireCollaborator().getId();
        LocalDateTime since = LocalDateTime.now().minusHours(PENDING_WINDOW_HOURS);

        return evaluationRepository
                .findPendingSessions(collaboratorId, since, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(CollaboratorSessionResponseDTO::new);
    }

    /** Avaliação de uma sessão específica, quando ela já foi respondida. */
    public Optional<EvaluationResponseDTO> findBySession(Long sessionId) {
        CollaboratorSession session = sessionRepository.findByIdScoped(Objects.requireNonNull(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));

        Long loggedCollaboratorId = Principals.collaborator().map(Collaborator::getId).orElse(null);
        if (loggedCollaboratorId != null && !session.getCollaborator().getId().equals(loggedCollaboratorId)) {
            throw new AccessDeniedException("Acesso negado. Você só pode consultar as suas próprias massagens.");
        }

        return evaluationRepository.findBySessionId(session.getId()).map(EvaluationResponseDTO::new);
    }

    /**
     * Filtro comum de listagem e resumo.
     *
     * <p>
     * O id do colaborador logado sobrepõe o que veio no filtro — é o mesmo
     * tratamento das sessões, e o que impede alguém de listar as avaliações de um
     * colega trocando um parâmetro na URL.
     */
    private Specification<Evaluation> toSpecification(EvaluationFilterDTO filter) {
        Long collaboratorId = filter != null ? filter.collaboratorId() : null;
        collaboratorId = Principals.collaborator().map(Collaborator::getId).orElse(collaboratorId);

        return Specification.allOf(
                EvaluationSpecifications.hasScore(filter != null ? filter.score() : null),
                EvaluationSpecifications.hasCollaborator(collaboratorId),
                EvaluationSpecifications.hasDepartment(filter != null ? filter.departmentId() : null),
                EvaluationSpecifications.betweenDates(filter != null ? filter.from() : null,
                        filter != null ? filter.to() : null));
    }

    /** Comentário em branco é ausência de comentário, não string vazia no banco. */
    private String normalizeComments(String comments) {
        if (comments == null) {
            return null;
        }
        String trimmed = comments.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
