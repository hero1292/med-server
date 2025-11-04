package org.aleksanyan.medserver.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateRecommendationRequest {

    @NotNull(message = "patientId обязателен")
    private Long patientId;

    @NotBlank(message = "title обязателен")
    private String title;

    @NotBlank(message = "dose обязательна")
    private String dose;

    @Positive(message = "frequencyPerDay должно быть > 0")
    private Integer frequencyPerDay;

    @Positive(message = "intervalHours должно быть > 0")
    private Integer intervalHours;

    @Positive(message = "durationDays должно быть > 0")
    private Integer durationDays;

    @NotNull(message = "startAt обязателен")
    private OffsetDateTime startAt;

    private String notes;
}
