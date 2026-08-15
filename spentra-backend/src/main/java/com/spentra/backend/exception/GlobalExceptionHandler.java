package com.spentra.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.spentra.backend.model.dto.exception.ApiExceptionResponse;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handles our custom business logic errors
    @ExceptionHandler(ApiRequestException.class)
    public ResponseEntity<ApiExceptionResponse> handleApiRequestException(ApiRequestException e) {
        log.warn("API request error status={} message={}", e.getStatus(), e.getMessage());
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .message(e.getMessage())
                .statusCode(e.getStatus().value())
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(response, e.getStatus());
    }

    // 2. Fallback for unexpected 500 errors (database down, null pointers, etc.)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiExceptionResponse> handleGeneralException(Exception e) {
        log.error("Unhandled backend error", e);
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .message("Something went wrong on our end. Please check backend logs for details.")
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.spentra.backend.model.dto.exception.ApiExceptionResponse> handleValidation(
            MethodArgumentNotValidException e) {
        // Get the first validation error message
        String errorMsg = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        log.warn("Validation error field={} message={}",
                e.getBindingResult().getFieldErrors().get(0).getField(), errorMsg);

        com.spentra.backend.model.dto.exception.ApiExceptionResponse response = com.spentra.backend.model.dto.exception.ApiExceptionResponse
                .builder()
                .message(errorMsg)
                .statusCode(org.springframework.http.HttpStatus.BAD_REQUEST.value())
                .timestamp(java.time.ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(response, org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}
