package org.aleksanyan.medserver.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDoctorProfileRequest {

    @Size(min = 3, max = 100)
    private String fullName;

    @Size(max = 128)
    private String specialty;

    @Size(max = 64)
    private String phone;
}
