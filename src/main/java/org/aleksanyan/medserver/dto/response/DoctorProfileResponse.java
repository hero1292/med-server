package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoctorProfileResponse {
    Long id;
    String fullName;
    String email;
    String specialty;
    String phone;
}
