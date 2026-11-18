
package com.beentteum.common.exception;

import org.springframework.validation.FieldError;

public record ValidationError(String field, String reason) {

    public static ValidationError from(FieldError error) {
        return new ValidationError(
                error.getField(),
                error.getDefaultMessage()
        );
    }
}
