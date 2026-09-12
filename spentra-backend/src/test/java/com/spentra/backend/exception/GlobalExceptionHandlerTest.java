package com.spentra.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.spentra.backend.model.dto.exception.ApiExceptionResponse;

class GlobalExceptionHandlerTest {

    @Test
    void maxUploadSizeExceeded_returnsTheReceiptSizeError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiExceptionResponse> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(2L * 1024 * 1024));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatusCode());
        assertEquals("Receipt images must be 2 MB or smaller.", response.getBody().getMessage());
    }
}
