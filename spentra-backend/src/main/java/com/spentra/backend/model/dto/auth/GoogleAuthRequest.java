package com.spentra.backend.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {
    @NotBlank(message = "ID Token is required")
    private String idToken;
}
