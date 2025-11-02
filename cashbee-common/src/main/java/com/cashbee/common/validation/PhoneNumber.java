package com.cashbee.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Annotation for validating Vietnamese phone numbers.
 * Format: 0XXXXXXXXX (10 or 11 digits starting with 0)
 *
 * @author CashBee Team
 */
@Documented
@Constraint(validatedBy = PhoneNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PhoneNumber {

    String message() default "Invalid phone number format. Must be Vietnamese format (0XXXXXXXXX)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Allow null values.
     */
    boolean optional() default false;
}
