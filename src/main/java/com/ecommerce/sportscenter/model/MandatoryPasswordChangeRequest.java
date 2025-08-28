package com.ecommerce.sportscenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandatoryPasswordChangeRequest {
    private String username;
    private String currentPassword;
    private String newPassword;
    private String repeatPassword;
}
