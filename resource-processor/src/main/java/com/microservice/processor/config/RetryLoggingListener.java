package com.microservice.processor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

/**
 * Custom RetryListener to log retry attempts and outcomes for operations that are retried using Spring Retry.
 */
@Slf4j
@Component("retryLoggingListener")
public class RetryLoggingListener implements RetryListener {

    /**
     * Logs a warning message each time a retry attempt fails, including the attempt count, operation name, and error message.
     *
     * @param context   The RetryContext containing information about the current retry attempt, including the attempt count and any attributes set on the context.
     * @param callback  The RetryCallback representing the operation being retried, which can be used to access additional information about the operation if needed.
     * @param throwable The exception that caused the retry attempt to fail.
     * @param <T>       The type of the return value of the retryable operation.
     * @param <E>       The type of the exception that can be thrown by the retryable operation.>
     */
    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("[Retry] Attempt {} failed for '{}': {}. Retrying...",
                context.getRetryCount(),
                context.getAttribute(RetryContext.NAME),
                throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable != null) {
            log.error("[Retry] All {} attempts exhausted for '{}': {}",
                    context.getRetryCount(),
                    context.getAttribute(RetryContext.NAME),
                    throwable.getMessage());
        } else {
            if (context.getRetryCount() > 0) {
                log.info("[Retry] Recovered after {} attempt(s) for '{}'",
                        context.getRetryCount(),
                        context.getAttribute(RetryContext.NAME));
            }
        }
    }
}
