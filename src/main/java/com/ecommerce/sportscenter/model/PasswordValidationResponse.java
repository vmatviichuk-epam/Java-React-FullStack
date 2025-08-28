package com.ecommerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordValidationResponse {
    private boolean valid;
    private List<String> errors;
    private List<String> requirements;
    private boolean requiresMandatoryChange;
}
