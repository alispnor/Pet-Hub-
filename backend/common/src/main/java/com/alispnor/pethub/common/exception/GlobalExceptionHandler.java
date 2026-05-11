package com.alispnor.pethub.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Tratador global de exceptions retornando RFC 7807 (Problem Details).
 *
 * Exemplo de resposta:
 * <pre>
 * {
 *   "type": "https://pethub.com/errors/resource-not-found",
 *   "title": "Recurso não encontrado",
 *   "status": 404,
 *   "detail": "Produto não encontrado: COLLAR-001",
 *   "instance": "/api/v1/catalog/products/COLLAR-001",
 *   "timestamp": "2026-05-11T20:00:00Z"
 * }
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TYPE = "https://pethub.com/errors/";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception em {}: {}", request.getRequestURI(), ex.getMessage());
        var problem = buildProblem(ex.getStatus(), titleFor(ex.getStatus()), ex.getMessage(), ex.getErrorType(), request);
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        log.debug("Validation error em {}: {}", request.getRequestURI(), errors);
        var problem = buildProblem(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos falharam na validação.", "validation-error", request);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.debug("Constraint violation em {}: {}", request.getRequestURI(), ex.getMessage());
        var problem = buildProblem(HttpStatus.BAD_REQUEST, "Dados inválidos", ex.getMessage(), "validation-error", request);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.info("Tentativa de login com credenciais inválidas em {}", request.getRequestURI());
        var problem = buildProblem(HttpStatus.UNAUTHORIZED, "Não autorizado",
                "Credenciais inválidas.", "invalid-credentials", request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.info("Falha de autenticação em {}: {}", request.getRequestURI(), ex.getMessage());
        var problem = buildProblem(HttpStatus.UNAUTHORIZED, "Não autorizado",
                "Autenticação falhou.", "authentication-failed", request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.info("Acesso negado em {}", request.getRequestURI());
        var problem = buildProblem(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.", "access-denied", request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        var problem = buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Erro interno do servidor. Tente novamente mais tarde.", "internal-error", request);
        return ResponseEntity.internalServerError().body(problem);
    }

    private ProblemDetail buildProblem(HttpStatus status, String title, String detail, String errorType,
                                       HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(BASE_TYPE + errorType));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", OffsetDateTime.now().toString());
        return problem;
    }

    private Map<String, String> toFieldError(FieldError fe) {
        return Map.of(
                "field", fe.getField(),
                "message", fe.getDefaultMessage() == null ? "valor inválido" : fe.getDefaultMessage()
        );
    }

    private String titleFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado";
            case CONFLICT -> "Conflito";
            case UNPROCESSABLE_ENTITY -> "Regra de negócio violada";
            case BAD_REQUEST -> "Requisição inválida";
            case UNAUTHORIZED -> "Não autorizado";
            case FORBIDDEN -> "Acesso negado";
            case TOO_MANY_REQUESTS -> "Muitas requisições";
            default -> status.getReasonPhrase();
        };
    }
}
