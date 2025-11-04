package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RegisterResponse {
    Long id;
    String email;
    String fullName;
    String role;
    String message;
}
