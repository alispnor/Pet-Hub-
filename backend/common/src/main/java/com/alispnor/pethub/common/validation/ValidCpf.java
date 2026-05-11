package com.alispnor.pethub.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida CPF brasileiro: 11 dígitos (sem máscara) e dígitos verificadores corretos.
 * Aceita null por padrão — combine com {@code @NotBlank} quando obrigatório.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface ValidCpf {

    String message() default "CPF inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
