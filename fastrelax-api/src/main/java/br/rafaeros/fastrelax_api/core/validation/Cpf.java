package br.rafaeros.fastrelax_api.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * CPF com 11 dígitos e verificadores válidos.
 *
 * <p>
 * Nulo e vazio passam: use com {@code @NotBlank} quando o campo for obrigatório,
 * ou sozinho quando for opcional (como no update de colaborador).
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

    String message() default "CPF inválido. Informe 11 dígitos, sem pontuação.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
