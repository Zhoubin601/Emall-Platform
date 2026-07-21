package com.emall.backend.dto;

import com.emall.backend.dto.order.CreateOrderItemRequest;
import com.emall.backend.dto.order.CreateOrderRequest;
import com.emall.backend.dto.user.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidOrderQuantity() {
        CreateOrderRequest request = new CreateOrderRequest(
                null, List.of(new CreateOrderItemRequest(1L, 101)));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("items[0].productCount");
    }

    @Test
    void rejectsMalformedRegistrationData() {
        RegisterRequest request = new RegisterRequest(
                "a", "", "bad-email", "short", "abc");

        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(5);
    }
}
