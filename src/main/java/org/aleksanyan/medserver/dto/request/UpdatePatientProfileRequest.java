package org.aleksanyan.medserver.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePatientProfileRequest {

    @Size(min = 3, max = 100)
    private String fullName;

    private LocalDate birthDate;

    @Size(max = 64)
    private String phone;

    @Size(max = 255)
    private String address;

    private Long doctorId;
}
