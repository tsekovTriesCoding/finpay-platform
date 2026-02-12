package com.finpay.payment.request;

import com.finpay.payment.request.event.MoneyRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that marks expired money requests and notifies both parties.
 * Runs every 5 minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoneyRequestExpirationJob {

    private final MoneyRequestRepository requestRepository;
    private final MoneyRequestEventProducer requestEventProducer;

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    @Transactional
    public void expirePendingRequests() {
        List<MoneyRequest> expired = requestRepository.findExpiredRequests(LocalDateTime.now());

        if (expired.isEmpty()) return;

        log.info("Expiring {} pending money requests", expired.size());

        for (MoneyRequest request : expired) {
            request.setStatus(MoneyRequest.RequestStatus.EXPIRED);
            requestRepository.save(request);

            requestEventProducer.publishRequestEvent(
                    MoneyRequestEvent.create(
                            request.getId(), request.getRequestReference(),
                            request.getRequesterUserId(), request.getPayerUserId(),
                            request.getAmount(), request.getCurrency(),
                            request.getDescription(),
                            MoneyRequestEvent.EventType.REQUEST_EXPIRED
                    )
            );
        }
    }
}
