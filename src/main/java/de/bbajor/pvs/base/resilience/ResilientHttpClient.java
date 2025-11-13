package de.bbajor.pvs.base.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Resilient HTTP client with retry logic and circuit breaker.
 * Provides retry and circuit breaker functionality for external API calls.
 */
@Component
@Slf4j
public class ResilientHttpClient {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final HttpClient httpClient;

    public ResilientHttpClient(
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Executes an HTTP request with retry and circuit breaker.
     * 
     * @param request the HTTP request
     * @param responseHandler the response handler
     * @param instanceName the resilience4j instance name (for retry and circuit breaker config)
     * @return the HTTP response
     * @throws Exception if the request fails after all retries
     */
    public <T> T executeWithResilience(
            HttpRequest request,
            Function<HttpResponse<String>, T> responseHandler,
            String instanceName) throws Exception {
        
        Retry retry = retryRegistry.retry(instanceName);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
        
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
                    try {
                        log.debug("Executing HTTP request to {} with resilience", request.uri());
                        HttpResponse<String> response = httpClient.send(
                                request, 
                                HttpResponse.BodyHandlers.ofString());
                        return responseHandler.apply(response);
                    } catch (Exception e) {
                        log.warn("HTTP request failed: {}", e.getMessage());
                        throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
                    }
                }));
        
        return decoratedSupplier.get();
    }

    /**
     * Executes an HTTP request with retry and circuit breaker (simple version).
     * 
     * @param request the HTTP request
     * @param instanceName the resilience4j instance name
     * @return the HTTP response body as string
     * @throws Exception if the request fails after all retries
     */
    public String executeWithResilience(HttpRequest request, String instanceName) throws Exception {
        return executeWithResilience(request, HttpResponse::body, instanceName);
    }

    /**
     * Gets the HTTP client instance.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }
}
