package com.microservice.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.gateway.dto.ErrorResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

@Component
@Order(-1)
@RequiredArgsConstructor
@Slf4j
public class GlobalGatewayExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            message = rse.getReason() != null ? rse.getReason() : "Request failed";
        } else if (ex instanceof ConnectException
                || (ex.getMessage() != null && ex.getMessage().contains("Connection refused"))) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service unavailable";
        } else if (ex instanceof IllegalStateException
                && ex.getMessage() != null && ex.getMessage().contains("No instances available")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service unavailable";
        } else {
            log.error("Unhandled gateway error: {}", ex.getMessage(), ex);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Internal server error";
        }

        ErrorResponseDto body = new ErrorResponseDto(String.valueOf(status.value()), message);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
}
