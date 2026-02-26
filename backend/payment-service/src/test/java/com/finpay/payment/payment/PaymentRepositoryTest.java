package com.finpay.payment.payment;

import com.finpay.payment.testconfig.TestMySQLContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("PaymentRepository Data JPA Tests")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    private Payment createPayment(UUID userId, String reference, Payment.PaymentStatus status) {
        return Payment.builder()
                .userId(userId)
                .transactionReference(reference)
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .status(status)
                .paymentMethod(Payment.PaymentMethod.CARD)
                .paymentType(Payment.PaymentType.PAYMENT)
                .processingFee(new BigDecimal("1.5000"))
                .totalAmount(new BigDecimal("101.5000"))
                .build();
    }

    @Nested
    @DisplayName("Save and Find")
    class SaveAndFind {

        @Test
        @DisplayName("should save and retrieve payment")
        void shouldSaveAndRetrieve() {
            Payment payment = createPayment(UUID.randomUUID(), "FP-TEST-001", Payment.PaymentStatus.PENDING);
            Payment saved = paymentRepository.save(payment);
            entityManager.flush();

            Optional<Payment> found = paymentRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getTransactionReference()).isEqualTo("FP-TEST-001");
            assertThat(found.get().getAmount()).isEqualByComparingTo("100.0000");
            assertThat(found.get().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should generate UUID on save")
        void shouldGenerateUUID() {
            Payment payment = createPayment(UUID.randomUUID(), "FP-TEST-002", Payment.PaymentStatus.PENDING);

            Payment saved = paymentRepository.save(payment);

            assertThat(saved.getId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Find By Transaction Reference")
    class FindByReference {

        @Test
        @DisplayName("should find by transaction reference")
        void shouldFindByReference() {
            UUID userId = UUID.randomUUID();
            paymentRepository.save(createPayment(userId, "FP-UNIQUE-REF", Payment.PaymentStatus.COMPLETED));

            Optional<Payment> found = paymentRepository.findByTransactionReference("FP-UNIQUE-REF");

            assertThat(found).isPresent();
            assertThat(found.get().getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("should return empty for non-existent reference")
        void shouldReturnEmptyForNonExistent() {
            assertThat(paymentRepository.findByTransactionReference("NONEXISTENT")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find By UserId")
    class FindByUserId {

        @Test
        @DisplayName("should find all payments for a user")
        void shouldFindByUserId() {
            UUID userId = UUID.randomUUID();
            paymentRepository.save(createPayment(userId, "FP-U1-001", Payment.PaymentStatus.PENDING));
            paymentRepository.save(createPayment(userId, "FP-U1-002", Payment.PaymentStatus.COMPLETED));
            paymentRepository.save(createPayment(UUID.randomUUID(), "FP-U2-001", Payment.PaymentStatus.PENDING));

            List<Payment> userPayments = paymentRepository.findByUserId(userId);

            assertThat(userPayments).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Find By Status")
    class FindByStatus {

        @Test
        @DisplayName("should find payments by status")
        void shouldFindByStatus() {
            UUID userId = UUID.randomUUID();
            paymentRepository.save(createPayment(userId, "FP-S1", Payment.PaymentStatus.PENDING));
            paymentRepository.save(createPayment(userId, "FP-S2", Payment.PaymentStatus.COMPLETED));
            paymentRepository.save(createPayment(userId, "FP-S3", Payment.PaymentStatus.PENDING));

            List<Payment> pendingPayments = paymentRepository.findByStatus(Payment.PaymentStatus.PENDING);
            List<Payment> completedPayments = paymentRepository.findByStatus(Payment.PaymentStatus.COMPLETED);

            assertThat(pendingPayments).hasSize(2);
            assertThat(completedPayments).hasSize(1);
        }

        @Test
        @DisplayName("should find by user ID and status")
        void shouldFindByUserIdAndStatus() {
            UUID userId = UUID.randomUUID();
            paymentRepository.save(createPayment(userId, "FP-US1", Payment.PaymentStatus.PENDING));
            paymentRepository.save(createPayment(userId, "FP-US2", Payment.PaymentStatus.COMPLETED));

            List<Payment> pending = paymentRepository.findByUserIdAndStatus(userId, Payment.PaymentStatus.PENDING);

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getTransactionReference()).isEqualTo("FP-US1");
        }
    }

    @Nested
    @DisplayName("Exists By Transaction Reference")
    class ExistsByReference {

        @Test
        @DisplayName("should return true when reference exists")
        void shouldReturnTrueWhenExists() {
            paymentRepository.save(createPayment(UUID.randomUUID(), "FP-EXISTS", Payment.PaymentStatus.PENDING));

            assertThat(paymentRepository.existsByTransactionReference("FP-EXISTS")).isTrue();
        }

        @Test
        @DisplayName("should return false when reference does not exist")
        void shouldReturnFalseWhenNotExists() {
            assertThat(paymentRepository.existsByTransactionReference("FP-NO")).isFalse();
        }
    }

    @Nested
    @DisplayName("Find By Date Range")
    class FindByDateRange {

        @Test
        @DisplayName("should find payments created within date range")
        void shouldFindByDateRange() {
            paymentRepository.save(createPayment(UUID.randomUUID(), "FP-DR1", Payment.PaymentStatus.PENDING));

            LocalDateTime start = LocalDateTime.now().minusMinutes(1);
            LocalDateTime end = LocalDateTime.now().plusMinutes(1);

            List<Payment> payments = paymentRepository.findByCreatedAtBetween(start, end);

            assertThat(payments).isNotEmpty();
        }
    }
}
