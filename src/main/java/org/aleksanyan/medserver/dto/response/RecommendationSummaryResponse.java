package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class RecommendationSummaryResponse {
    Long id;
    String title;
    String dose;
    Integer frequencyPerDay;
    Integer intervalHours;
    Integer durationDays;
    OffsetDateTime startAt;
    String notes;
    Long patientId;
    String patientName;
    int scheduleCount;
}
