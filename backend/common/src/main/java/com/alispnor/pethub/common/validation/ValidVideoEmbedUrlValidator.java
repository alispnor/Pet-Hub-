package com.alispnor.pethub.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidVideoEmbedUrlValidator implements ConstraintValidator<ValidVideoEmbedUrl, String> {

    private static final Pattern PATTERN = Pattern.compile(
            "^https://(www\\.)?(youtube\\.com/watch\\?v=[\\w-]+|youtu\\.be/[\\w-]+|vimeo\\.com/\\d+)([&?][\\w-=&]*)?$"
    );
    private static final int MAX_LENGTH = 500;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (value.length() > MAX_LENGTH) {
            return false;
        }
        return PATTERN.matcher(value).matches();
    }
}
