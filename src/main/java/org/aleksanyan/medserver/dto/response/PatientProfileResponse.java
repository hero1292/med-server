package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class PatientProfileResponse {
    Long id;
    String fullName;
    String email;
    LocalDate birthDate;
    String phone;
    String address;
    Long doctorId;
    String doctorName;
}
