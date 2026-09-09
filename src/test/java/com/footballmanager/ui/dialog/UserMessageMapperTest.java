package com.footballmanager.ui.dialog;

import com.footballmanager.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMessageMapperTest {
    private final UserMessageMapper mapper = new UserMessageMapper();

    @Test
    void preservesFriendlyApplicationMessages() {
        assertEquals(
                "Shirt number must be between 1 and 99",
                mapper.messageFor(new ValidationException("Shirt number must be between 1 and 99"))
        );
    }

    @Test
    void hidesUnexpectedTechnicalDetails() {
        assertEquals(
                "The operation could not be completed. Please try again.",
                mapper.messageFor(new IllegalStateException("secret stack detail"))
        );
    }
}
