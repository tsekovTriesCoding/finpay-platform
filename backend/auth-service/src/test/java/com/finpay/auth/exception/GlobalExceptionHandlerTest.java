package com.finpay.auth.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Nested
    @DisplayName("BadCredentialsException")
    class BadCredentialsTests {

        @Test
        @DisplayName("should return 401 with custom message for suspended account")
        void shouldReturnCustomMessageForSuspendedAccount() {
            var ex = new BadCredentialsException("Account is suspended");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBadCredentialsException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Account is suspended");
            assertThat(response.getBody().error()).isEqualTo("Unauthorized");
            assertThat(response.getBody().status()).isEqualTo(401);
        }

        @Test
        @DisplayName("should return 401 with custom message for invalid credentials")
        void shouldReturnCustomMessageForInvalidCredentials() {
            var ex = new BadCredentialsException("Invalid email or password");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBadCredentialsException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
        }

        @Test
        @DisplayName("should return 401 with custom message for locked account")
        void shouldReturnCustomMessageForLockedAccount() {
            var ex = new BadCredentialsException("Account is locked");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBadCredentialsException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Account is locked");
        }

        @Test
        @DisplayName("should preserve the exact exception message")
        void shouldPreserveExactExceptionMessage() {
            String customMessage = "Some specific error reason";
            var ex = new BadCredentialsException(customMessage);

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBadCredentialsException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo(customMessage);
        }

        @Test
        @DisplayName("should include timestamp in response")
        void shouldIncludeTimestampInResponse() {
            var ex = new BadCredentialsException("test");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleBadCredentialsException(ex);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("InvalidTokenException")
    class InvalidTokenTests {

        @Test
        @DisplayName("should return 401 with token error message")
        void shouldReturnTokenErrorMessage() {
            var ex = new InvalidTokenException("Invalid or expired token");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleInvalidTokenException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Invalid or expired token");
        }
    }

    @Nested
    @DisplayName("UserAlreadyExistsException")
    class UserAlreadyExistsTests {

        @Test
        @DisplayName("should return 409 Conflict")
        void shouldReturn409Conflict() {
            var ex = new UserAlreadyExistsException("User with email exists");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleUserAlreadyExistsException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("User with email exists");
        }
    }

    @Nested
    @DisplayName("Generic Exception")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 for unexpected errors")
        void shouldReturn500ForUnexpectedErrors() {
            var ex = new RuntimeException("Something went wrong");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleGenericException(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("Something went wrong");
            assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        }
    }
}
