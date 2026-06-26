package com.spentra.backend.model.dto.exception;

import lombok.Builder;
import lombok.Data;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
public class ApiExceptionResponse {
    private final String message;

    // Example: "NOT_FOUND"
    private final String error;

    private final int statusCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final ZonedDateTime timestamp;
}