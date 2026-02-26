package com.finpay.payment.transfer;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestMySQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("MoneyTransferRepository Data JPA Tests")
class MoneyTransferRepositoryTest {

    @Autowired
    private MoneyTransferRepository transferRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
    }

    private MoneyTransfer createTransfer(UUID senderId, UUID recipientId, String ref,
                                          MoneyTransfer.TransferStatus status,
                                          MoneyTransfer.SagaStatus sagaStatus) {
        return MoneyTransfer.builder()
                .transactionReference(ref)
                .senderUserId(senderId)
                .recipientUserId(recipientId)
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .description("Test transfer")
                .transferType(MoneyTransfer.TransferType.SEND)
                .status(status)
                .sagaStatus(sagaStatus)
                .fundsReserved(false)
                .fundsDeducted(false)
                .fundsCredit(false)
                .notificationSent(false)
                .compensationRequired(false)
                .compensationCompleted(false)
                .build();
    }

    @Nested
    @DisplayName("Save and Find")
    class SaveAndFind {

        @Test
        @DisplayName("should save and retrieve transfer")
        void shouldSaveAndRetrieve() {
            MoneyTransfer transfer = createTransfer(
                    UUID.randomUUID(), UUID.randomUUID(), "TRF-001",
                    MoneyTransfer.TransferStatus.PROCESSING, MoneyTransfer.SagaStatus.STARTED
            );

            MoneyTransfer saved = transferRepository.save(transfer);
            entityManager.flush();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getTransactionReference()).isEqualTo("TRF-001");

            MoneyTransfer found = entityManager.find(MoneyTransfer.class, saved.getId());
            assertThat(found.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Find By Reference")
    class FindByReference {

        @Test
        @DisplayName("should find by transaction reference")
        void shouldFindByReference() {
            transferRepository.save(createTransfer(
                    UUID.randomUUID(), UUID.randomUUID(), "TRF-UNIQUE",
                    MoneyTransfer.TransferStatus.PROCESSING, MoneyTransfer.SagaStatus.STARTED
            ));

            Optional<MoneyTransfer> found = transferRepository.findByTransactionReference("TRF-UNIQUE");

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should return empty for non-existent reference")
        void shouldReturnEmpty() {
            assertThat(transferRepository.findByTransactionReference("NONE")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find By User Participant")
    class FindByParticipant {

        @Test
        @DisplayName("should find transfers where user is sender or recipient")
        void shouldFindAsParticipant() {
            UUID userId = UUID.randomUUID();
            UUID otherUser1 = UUID.randomUUID();
            UUID otherUser2 = UUID.randomUUID();

            // User is sender
            transferRepository.save(createTransfer(userId, otherUser1, "TRF-SENDER",
                    MoneyTransfer.TransferStatus.COMPLETED, MoneyTransfer.SagaStatus.COMPLETED));
            // User is recipient
            transferRepository.save(createTransfer(otherUser2, userId, "TRF-RECIPIENT",
                    MoneyTransfer.TransferStatus.COMPLETED, MoneyTransfer.SagaStatus.COMPLETED));
            // User is not involved
            transferRepository.save(createTransfer(otherUser1, otherUser2, "TRF-OTHER",
                    MoneyTransfer.TransferStatus.COMPLETED, MoneyTransfer.SagaStatus.COMPLETED));

            Page<MoneyTransfer> results = transferRepository.findByUserIdAsParticipant(
                    userId, PageRequest.of(0, 10));

            assertThat(results.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Find By Status")
    class FindByStatus {

        @Test
        @DisplayName("should find transfers by status")
        void shouldFindByStatus() {
            UUID s = UUID.randomUUID();
            UUID r = UUID.randomUUID();
            transferRepository.save(createTransfer(s, r, "TRF-P1",
                    MoneyTransfer.TransferStatus.PROCESSING, MoneyTransfer.SagaStatus.STARTED));
            transferRepository.save(createTransfer(s, r, "TRF-C1",
                    MoneyTransfer.TransferStatus.COMPLETED, MoneyTransfer.SagaStatus.COMPLETED));

            List<MoneyTransfer> processing = transferRepository.findByStatus(MoneyTransfer.TransferStatus.PROCESSING);
            List<MoneyTransfer> completed = transferRepository.findByStatus(MoneyTransfer.TransferStatus.COMPLETED);

            assertThat(processing).hasSize(1);
            assertThat(completed).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Find Pending Compensations")
    class FindPendingCompensations {

        @Test
        @DisplayName("should find transfers needing compensation")
        void shouldFindPendingCompensations() {
            UUID s = UUID.randomUUID();
            UUID r = UUID.randomUUID();

            MoneyTransfer needsCompensation = createTransfer(s, r, "TRF-COMP",
                    MoneyTransfer.TransferStatus.FAILED, MoneyTransfer.SagaStatus.FAILED);
            needsCompensation.setCompensationRequired(true);
            needsCompensation.setCompensationCompleted(false);
            transferRepository.save(needsCompensation);

            MoneyTransfer alreadyComp = createTransfer(s, r, "TRF-DONE",
                    MoneyTransfer.TransferStatus.COMPENSATED, MoneyTransfer.SagaStatus.COMPENSATED);
            alreadyComp.setCompensationRequired(true);
            alreadyComp.setCompensationCompleted(true);
            transferRepository.save(alreadyComp);

            List<MoneyTransfer> pending = transferRepository.findPendingCompensations(
                    MoneyTransfer.SagaStatus.FAILED);

            assertThat(pending).hasSize(1);
            assertThat(pending.get(0).getTransactionReference()).isEqualTo("TRF-COMP");
        }
    }

    @Nested
    @DisplayName("SAGA State Persistence")
    class SagaPersistence {

        @Test
        @DisplayName("should persist all SAGA flags")
        void shouldPersistSagaFlags() {
            MoneyTransfer transfer = createTransfer(UUID.randomUUID(), UUID.randomUUID(), "TRF-SAGA",
                    MoneyTransfer.TransferStatus.COMPLETED, MoneyTransfer.SagaStatus.COMPLETED);
            transfer.setFundsReserved(true);
            transfer.setFundsDeducted(true);
            transfer.setFundsCredit(true);
            transfer.setNotificationSent(true);

            MoneyTransfer saved = transferRepository.save(transfer);
            Optional<MoneyTransfer> found = transferRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().isFundsReserved()).isTrue();
            assertThat(found.get().isFundsDeducted()).isTrue();
            assertThat(found.get().isFundsCredit()).isTrue();
            assertThat(found.get().isNotificationSent()).isTrue();
        }
    }
}
