package com.microservice.processor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@Slf4j
@RefreshScope
@Service
public class ProcessorService {

    @Value("${processor.response.timeout-ms:5000}")
    private long timeoutMs;

    public long getTimeoutMs() {
        log.info("Current processor.response.timeout-ms = {}", timeoutMs);
        return timeoutMs;
    }
}