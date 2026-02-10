package com.finpay.auth.service;

import com.finpay.auth.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Client for calling user-service via Eureka service discovery.
 * Uses LoadBalancerClient to manually resolve the service instance,
 * avoiding the circular dependency that @LoadBalanced causes with Eureka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final LoadBalancerClient loadBalancerClient;
    private final RestClient restClient;

    /**
     * Fetches the full user profile from user-service.
     * This returns all fields including address, profileImageUrl, etc.
     * that the auth-service's local user_credentials table doesn't store.
     */
    public UserDto getUserProfile(UUID userId) {
        try {
            ServiceInstance instance = loadBalancerClient.choose("user-service");
            if (instance == null) {
                log.warn("No user-service instance available via Eureka");
                return null;
            }

            String url = instance.getUri() + "/api/v1/users/" + userId;
            log.debug("Fetching full user profile from user-service at: {}", url);

            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(UserDto.class);
        } catch (Exception e) {
            log.warn("Failed to fetch user profile from user-service for user: {}. Falling back to local data. Error: {}",
                    userId, e.getMessage());
            return null;
        }
    }
}
