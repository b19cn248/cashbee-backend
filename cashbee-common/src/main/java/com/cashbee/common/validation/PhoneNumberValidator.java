package com.cashbee.common.validation;

import com.cashbee.common.constant.AppConstants;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for Vietnamese phone numbers.
 * Validates format: 0XXXXXXXXX (10-11 digits starting with 0)
 *
 * @author CashBee Team
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final Pattern PHONE_PATTERN = Pattern.compile(AppConstants.PHONE_REGEX);
    private boolean optional;

    @Override
    public void initialize(PhoneNumber annotation) {
        this.optional = annotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // If optional and value is null or empty, consider it valid
        if (optional && StringUtils.isBlank(value)) {
            return true;
        }

        // If not optional and value is null or empty, consider it invalid
        if (StringUtils.isBlank(value)) {
            return false;
        }

        // Validate against pattern
        return PHONE_PATTERN.matcher(value.trim()).matches();
    }
}
