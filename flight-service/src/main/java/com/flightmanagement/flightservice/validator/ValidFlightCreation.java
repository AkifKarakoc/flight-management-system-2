package com.flightmanagement.flightservice.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = FlightCreationValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFlightCreation {
    String message() default "Invalid flight creation request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 