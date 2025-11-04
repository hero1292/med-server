package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class RecommendationResponse {
    Long id;
    String title;
    String dose;
    Integer frequencyPerDay;
    Integer intervalHours;
    Integer durationDays;
    OffsetDateTime startAt;
    String notes;
    Long doctorId;
    Long patientId;
    List<ScheduleItemResponse> schedule;
}
