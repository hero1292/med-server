package org.aleksanyan.medserver.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh токен обязателен")
    private String refreshToken;
}
