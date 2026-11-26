package com.example.Investment.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

// @RestControllerAdvice applies to every @RestController in the app - one place
// to turn exceptions into clean JSON error responses instead of try/catch in each endpoint.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(StockNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientDataException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientData(InsufficientDataException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("error", ex.getMessage()));
    }

    // Thrown by RestTemplate when the yfinance sidecar is unreachable or errors out
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleUpstreamError(RestClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("error", "Could not reach the market data provider. Please try again shortly."));
    }
}
