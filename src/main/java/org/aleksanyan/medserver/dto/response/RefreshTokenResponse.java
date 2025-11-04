package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RefreshTokenResponse {
    String accessToken;
    String refreshToken;
    String tokenType;
    String message;
}
