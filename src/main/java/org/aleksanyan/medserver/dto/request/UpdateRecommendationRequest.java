package org.aleksanyan.medserver.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class UpdateRecommendationRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String dose;

    @Positive
    private Integer frequencyPerDay;

    @Positive
    private Integer intervalHours;

    @Positive
    private Integer durationDays;

    @NotNull
    private OffsetDateTime startAt;

    private String notes;
}
