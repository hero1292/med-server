package org.aleksanyan.medserver.dto.response;

import lombok.Builder;
import lombok.Value;
import java.time.OffsetDateTime;

@Value
@Builder
public class ScheduleItemResponse {
    Long id;
    OffsetDateTime plannedAt;
    Boolean taken;
    OffsetDateTime takenAt;
    String note;
    String recommendationTitle;
    String recommendationDose;
    String doctorName;
}
