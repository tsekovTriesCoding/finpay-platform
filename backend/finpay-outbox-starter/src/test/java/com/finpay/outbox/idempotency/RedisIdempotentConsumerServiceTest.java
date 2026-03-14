package com.finpay.outbox.idempotency;

import com.finpay.outbox.OutboxProperties;
import com.finpay.outbox.entity.ProcessedEvent;
import com.finpay.outbox.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisIdempotentConsumerService Unit Tests")
class RedisIdempotentConsumerServiceTest {

    @Mock private ProcessedEventRepository repository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Spy  private OutboxProperties properties;

    @InjectMocks
    private RedisIdempotentConsumerService service;

    @Nested
    @DisplayName("isDuplicate")
    class IsDuplicateTests {

        @Test
        @DisplayName("should return true if event exists in Redis")
        void shouldReturnTrueIfInRedis() {
            when(redisTemplate.hasKey("outbox:processed:event-123")).thenReturn(true);

            assertThat(service.isDuplicate("event-123")).isTrue();
            verify(repository, never()).existsById(any());
        }

        @Test
        @DisplayName("should fall back to DB if Redis miss, and backfill Redis")
        void shouldFallBackToDbAndBackfill() {
            when(redisTemplate.hasKey("outbox:processed:event-456")).thenReturn(false);
            when(repository.existsById("event-456")).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            assertThat(service.isDuplicate("event-456")).isTrue();
            verify(valueOperations).set(eq("outbox:processed:event-456"), eq("backfill"), any(Duration.class));
        }

        @Test
        @DisplayName("should return false if not in Redis or DB")
        void shouldReturnFalseIfNotFound() {
            when(redisTemplate.hasKey("outbox:processed:event-789")).thenReturn(false);
            when(repository.existsById("event-789")).thenReturn(false);

            assertThat(service.isDuplicate("event-789")).isFalse();
        }

        @Test
        @DisplayName("should return false for null eventId")
        void shouldReturnFalseForNull() {
            assertThat(service.isDuplicate(null)).isFalse();
        }

        @Test
        @DisplayName("should fall back to DB if Redis throws exception")
        void shouldFallBackIfRedisError() {
            when(redisTemplate.hasKey("outbox:processed:event-err")).thenThrow(new RuntimeException("Redis down"));
            when(repository.existsById("event-err")).thenReturn(false);

            assertThat(service.isDuplicate("event-err")).isFalse();
        }
    }

    @Nested
    @DisplayName("markProcessed")
    class MarkProcessedTests {

        @Test
        @DisplayName("should write to both DB and Redis")
        void shouldWriteToBothDbAndRedis() {
            when(repository.existsById("event-new")).thenReturn(false);
            when(repository.saveAndFlush(any(ProcessedEvent.class))).thenReturn(null);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            service.markProcessed("event-new", "test-consumer");

            verify(repository).saveAndFlush(any(ProcessedEvent.class));
            verify(valueOperations).set(
                    eq("outbox:processed:event-new"),
                    eq("test-consumer"),
                    eq(Duration.ofDays(14))
            );
        }

        @Test
        @DisplayName("should skip if already exists in DB")
        void shouldSkipIfAlreadyExists() {
            when(repository.existsById("event-existing")).thenReturn(true);

            service.markProcessed("event-existing", "test-consumer");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("should not fail if Redis write fails")
        void shouldNotFailIfRedisWriteFails() {
            when(repository.existsById("event-redis-fail")).thenReturn(false);
            when(repository.saveAndFlush(any(ProcessedEvent.class))).thenReturn(null);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RuntimeException("Redis down")).when(valueOperations)
                    .set(any(), any(), any(Duration.class));

            // Should not throw
            service.markProcessed("event-redis-fail", "test-consumer");

            verify(repository).saveAndFlush(any(ProcessedEvent.class));
        }
    }
}
