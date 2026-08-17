package br.rafaeros.fastrelax_api.core.exceptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Quantas linhas do stacktrace vão para a resposta quando os detalhes estão expostos. */
    private static final int STACKTRACE_LINES = 15;

    /**
     * Detalhes internos na resposta ajudam no desenvolvimento, mas revelam estrutura
     * do código e do banco. Mantenha desligado em produção — o stacktrace completo
     * continua indo para o log de qualquer forma.
     */
    @Value("${app.errors.include-details:false}")
    private boolean includeDetails;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponseDTO.warning(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.warning(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponseDTO.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Dados de entrada inválidos.", errors));
    }

    // Precede o handler genérico, que devolveria 500 para um verbo HTTP errado.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        // Guardado numa variável: chamar o getter duas vezes impede o compilador de
        // provar que a segunda chamada não é nula.
        var supportedMethods = ex.getSupportedHttpMethods();
        String supported = supportedMethods == null ? "" : supportedMethods.toString();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponseDTO.warning("Método " + ex.getMethod()
                        + " não é suportado nesta rota. Use: " + supported));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponseDTO.warning("Content-Type não suportado. Use application/json."));
    }

    /**
     * {@code ?sort=} apontando para um campo que não existe na entidade. Vem do
     * cliente, então é 400 — não uma falha do servidor.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleInvalidSortProperty(PropertyReferenceException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.warning("Campo de ordenação inválido: '" + ex.getPropertyName()
                        + "'. Use um campo existente do recurso."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponseDTO.warning("Arquivo excede o tamanho máximo permitido."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.warning("Parâmetro obrigatório ausente: " + ex.getParameterName()));
    }

    /** Ex.: id não numérico na URL ou data em formato inválido na query. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.warning("Valor inválido para o parâmetro '" + ex.getName() + "'."));
    }

    /** Violação de constraint que passou pelas checagens de negócio. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiResponseDTO<>("warning",
                        "Operação viola uma restrição de dados. Verifique duplicidades.",
                        null, null, describe(ex), java.time.LocalDateTime.now()));
    }

    /**
     * Falha de autenticação sempre com a mesma mensagem e o mesmo status.
     *
     * <p>
     * Distinguir "email não existe" de "senha incorreta" permitiria descobrir quais
     * emails estão cadastrados testando um por um. O motivo real fica só no log.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Falha de autenticação: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponseDTO.warning("Email ou senha incorretos."));
    }

    // Precede o handler de RuntimeException, que devolveria 400 para uma negativa de acesso.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponseDTO.warning(ex.getMessage()));
    }

    // Enum inválido ou JSON malformado no corpo: sem isto, a mensagem interna do
    // Jackson vazaria na resposta pelo handler genérico.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.warning("Corpo da requisição inválido. Verifique os valores enviados."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGenericException(Exception ex) {
        log.error("Erro inesperado no servidor", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponseDTO<>("error",
                        "Ocorreu um erro inesperado no servidor. Tente novamente mais tarde.",
                        null, null, describe(ex), java.time.LocalDateTime.now()));
    }

    /**
     * Classe, mensagem e início do stacktrace — só quando
     * {@code app.errors.include-details} está ligado. Retorna null caso contrário,
     * e o campo some da resposta por causa do {@code @JsonInclude(NON_NULL)}.
     */
    private List<String> describe(Throwable ex) {
        if (!includeDetails) {
            return null;
        }
        List<String> details = new ArrayList<>();
        for (Throwable cause = ex; cause != null && details.size() < 3; cause = cause.getCause()) {
            details.add(cause.getClass().getName() + ": " + cause.getMessage());
            if (cause.getCause() == cause) {
                break;
            }
        }
        Arrays.stream(ex.getStackTrace())
                .limit(STACKTRACE_LINES)
                .map(element -> String.valueOf(element))
                .forEach(line -> details.add(line));
        return details;
    }
}
