package com.spentra.backend.model.dto.ai;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuickAddRequest {
    @Size(min = 3, max = 500, message = "Prompt must be between 3 and 500 characters.")
    private String prompt;
}
