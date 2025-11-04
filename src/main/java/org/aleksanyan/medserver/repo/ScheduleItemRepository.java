package org.aleksanyan.medserver.repo;

import org.aleksanyan.medserver.domain.PatientProfile;
import org.aleksanyan.medserver.domain.Recommendation;
import org.aleksanyan.medserver.domain.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {
    List<ScheduleItem> findAllByRecommendationAndPlannedAtAfter(Recommendation recommendation, OffsetDateTime time);
    List<ScheduleItem> findAllByPatientAndPlannedAtBetweenOrderByPlannedAtAsc(
            PatientProfile patient,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
