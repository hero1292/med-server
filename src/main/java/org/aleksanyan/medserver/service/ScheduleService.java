package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.domain.Recommendation;
import org.aleksanyan.medserver.domain.Role;
import org.aleksanyan.medserver.domain.ScheduleItem;
import org.aleksanyan.medserver.domain.User;
import org.aleksanyan.medserver.dto.request.TakeMedicineRequest;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.ScheduleItemRepository;
import org.aleksanyan.medserver.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientRepository;
    private final ScheduleItemRepository scheduleRepository;

    @Transactional
    public List<ScheduleItem> generateSchedule(Recommendation rec) {
        List<ScheduleItem> schedule = new ArrayList<>();
        OffsetDateTime start = rec.getStartAt();
        int totalDays = rec.getDurationDays() != null ? rec.getDurationDays() : 1;

        Integer intervalHours = rec.getIntervalHours();
        Integer frequencyPerDay = rec.getFrequencyPerDay();

        if (intervalHours != null && intervalHours > 24) {
            OffsetDateTime current = start;
            OffsetDateTime end = start.plusDays(totalDays);
            while (current.isBefore(end)) {
                ScheduleItem item = ScheduleItem.builder()
                        .recommendation(rec)
                        .patient(rec.getPatient())
                        .plannedAt(current)
                        .taken(false)
                        .build();
                schedule.add(item);
                current = current.plusHours(intervalHours);
            }
            return scheduleRepository.saveAll(schedule);
        }

        int intervalsPerDay;
        if (frequencyPerDay != null && frequencyPerDay > 0) {
            intervalsPerDay = frequencyPerDay;
        } else if (intervalHours != null && intervalHours > 0) {
            intervalsPerDay = Math.max(1, 24 / intervalHours);
        } else {
            intervalsPerDay = 1;
        }

        for (int day = 0; day < totalDays; day++) {
            OffsetDateTime dayStart = start.plusDays(day);

            for (int i = 0; i < intervalsPerDay; i++) {
                OffsetDateTime plannedAt;
                if (intervalHours != null && intervalHours > 0) {
                    plannedAt = dayStart.plusHours(intervalHours * i);
                } else {
                    plannedAt = dayStart.plusHours((long) (24 / intervalsPerDay) * i);
                }

                if (plannedAt.isBefore(start)) continue;

                ScheduleItem item = ScheduleItem.builder()
                        .recommendation(rec)
                        .patient(rec.getPatient())
                        .plannedAt(plannedAt)
                        .taken(false)
                        .build();
                schedule.add(item);
            }
        }

        return scheduleRepository.saveAll(schedule);
    }



    public void deleteFutureItems(Recommendation recommendation) {
        var now = OffsetDateTime.now();
        var futureItems = scheduleRepository.findAllByRecommendationAndPlannedAtAfter(recommendation, now);
        scheduleRepository.deleteAll(futureItems);
    }

    @Transactional(readOnly = true)
    public List<ScheduleItemResponse> getPatientSchedule(String email, OffsetDateTime from, OffsetDateTime to) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.PATIENT) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        var patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        OffsetDateTime fromDate = from != null ? from : OffsetDateTime.now().minusDays(7);
        OffsetDateTime toDate = to != null ? to : OffsetDateTime.now().plusDays(7);

        var items = scheduleRepository.findAllByPatientAndPlannedAtBetweenOrderByPlannedAtAsc(patient, fromDate, toDate);

        return items.stream()
                .map(item -> ScheduleItemResponse.builder()
                        .id(item.getId())
                        .plannedAt(item.getPlannedAt())
                        .taken(item.getTaken())
                        .takenAt(item.getTakenAt())
                        .note(item.getNote())
                        .recommendationTitle(item.getRecommendation().getTitle())
                        .recommendationDose(item.getRecommendation().getDose())
                        .doctorName(item.getRecommendation().getDoctor().getUser().getFullName())
                        .build())
                .toList();
    }

    @Transactional
    public ScheduleItemResponse markItemAsTaken(String email, Long itemId, TakeMedicineRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.PATIENT) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        var patient = patientRepository.findByUser(user)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        ScheduleItem item = scheduleRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));

        if (!item.getPatient().getId().equals(patient.getId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        item.setTaken(true);
        item.setTakenAt(OffsetDateTime.now());
        if (request != null && request.getNote() != null) {
            item.setNote(request.getNote());
        }

        scheduleRepository.save(item);

        return ScheduleItemResponse.builder()
                .id(item.getId())
                .plannedAt(item.getPlannedAt())
                .taken(item.getTaken())
                .takenAt(item.getTakenAt())
                .note(item.getNote())
                .recommendationTitle(item.getRecommendation().getTitle())
                .recommendationDose(item.getRecommendation().getDose())
                .doctorName(item.getRecommendation().getDoctor().getUser().getFullName())
                .build();
    }
}
