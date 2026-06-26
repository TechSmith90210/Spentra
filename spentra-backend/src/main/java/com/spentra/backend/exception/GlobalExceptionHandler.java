package com.spentra.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.spentra.backend.model.dto.exception.ApiExceptionResponse;

import java.time.ZonedDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handles our custom business logic errors
    @ExceptionHandler(ApiRequestException.class)
    public ResponseEntity<ApiExceptionResponse> handleApiRequestException(ApiRequestException e) {
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
        ApiExceptionResponse response = ApiExceptionResponse.builder()
                .message("Something went wrong on our end.")
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

        com.spentra.backend.model.dto.exception.ApiExceptionResponse response = com.spentra.backend.model.dto.exception.ApiExceptionResponse
                .builder()
                .message(errorMsg)
                .statusCode(org.springframework.http.HttpStatus.BAD_REQUEST.value())
                .timestamp(java.time.ZonedDateTime.now())
                .build();

        return new ResponseEntity<>(response, org.springframework.http.HttpStatus.BAD_REQUEST);
    }
}