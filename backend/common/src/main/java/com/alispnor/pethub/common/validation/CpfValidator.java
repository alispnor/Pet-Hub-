package com.alispnor.pethub.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        var digits = value.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return false;
        }
        if (digits.chars().distinct().count() == 1) {
            // 111.111.111-11, 222... etc são inválidos.
            return false;
        }
        return checkDigit(digits, 9) == digitAt(digits, 9)
                && checkDigit(digits, 10) == digitAt(digits, 10);
    }

    private int checkDigit(String digits, int length) {
        var sum = 0;
        for (var i = 0; i < length; i++) {
            sum += digitAt(digits, i) * (length + 1 - i);
        }
        var remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    private int digitAt(String s, int index) {
        return s.charAt(index) - '0';
    }
}
