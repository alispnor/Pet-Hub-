package com.alispnor.pethub.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida código NCM: exatamente 8 dígitos numéricos.
 * Aceita null/blank — combine com {@code @NotBlank} quando obrigatório.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NcmValidator.class)
public @interface ValidNcm {

    String message() default "NCM deve ter exatamente 8 dígitos numéricos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
