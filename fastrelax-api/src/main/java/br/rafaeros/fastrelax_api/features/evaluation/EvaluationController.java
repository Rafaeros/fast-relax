package br.rafaeros.fastrelax_api.features.evaluation;

import java.util.Objects;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionResponseDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.CreateEvaluationDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationFilterDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationResponseDTO;
import br.rafaeros.fastrelax_api.features.evaluation.dto.EvaluationSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/evaluations")
@RequiredArgsConstructor
@Tag(name = "Avaliações das massagens")
public class EvaluationController {

    private final EvaluationService evaluationService;

    /**
     * Listagem. A rota é a mesma para os dois públicos: o serviço restringe o
     * colaborador às próprias avaliações, e o RH vê a empresa inteira.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista as avaliações; o colaborador vê apenas as próprias")
    public ResponseEntity<ApiResponseDTO<Page<EvaluationResponseDTO>>> listAll(
            @ParameterObject EvaluationFilterDTO filter,
            @ParameterObject @PageableDefault(size = 10, sort = "evaluationDate") Pageable pageable) {
        Page<EvaluationResponseDTO> evaluations = evaluationService.findAll(filter, Objects.requireNonNull(pageable));
        return ResponseEntity.ok(ApiResponseDTO.success(evaluations, "Avaliações listadas com sucesso"));
    }

    /** Média, distribuição das notas e adesão — o cabeçalho da tela do RH. */
    @GetMapping("/summary")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Resumo das avaliações da empresa no período filtrado")
    public ResponseEntity<ApiResponseDTO<EvaluationSummaryDTO>> summary(
            @ParameterObject EvaluationFilterDTO filter) {
        return ResponseEntity.ok(ApiResponseDTO.success(evaluationService.summary(filter),
                "Resumo das avaliações"));
    }

    /**
     * Massagem que ainda espera nota. Devolve {@code data: null} quando não há
     * nenhuma, para o app decidir entre abrir o modal e seguir direto.
     */
    @GetMapping("/me/pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Massagem concluída do colaborador logado que ainda não foi avaliada")
    public ResponseEntity<ApiResponseDTO<CollaboratorSessionResponseDTO>> myPending() {
        return evaluationService.findMyPendingSession()
                .map(session -> ResponseEntity.ok(ApiResponseDTO.success(session, "Massagem aguardando avaliação")))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponseDTO.success("Nenhuma massagem aguardando avaliação")));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Avaliação de uma sessão; data nula quando ainda não foi avaliada")
    public ResponseEntity<ApiResponseDTO<EvaluationResponseDTO>> bySession(@PathVariable Long sessionId) {
        return evaluationService.findBySession(sessionId)
                .map(evaluation -> ResponseEntity.ok(ApiResponseDTO.success(evaluation, "Avaliação encontrada")))
                .orElseGet(() -> ResponseEntity.ok(ApiResponseDTO.success("Massagem ainda não avaliada")));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Registra a nota da própria massagem concluída")
    public ResponseEntity<ApiResponseDTO<EvaluationResponseDTO>> create(
            @RequestBody @Valid CreateEvaluationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(evaluationService.create(dto), "Obrigado pela sua avaliação!"));
    }
}
