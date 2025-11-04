package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.DoctorProfileRepository;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.ScheduleItemRepository;
import org.aleksanyan.medserver.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorRepository;
    private final PatientProfileRepository patientRepository;
    private final ScheduleItemRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<ScheduleItemResponse> getPatientSchedule(
            String doctorEmail,
            Long patientId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCTOR_NOT_FOUND));
        if (doctorUser.getRole() != Role.DOCTOR)
            throw new ApiException(ErrorCode.ACCESS_DENIED);

        var doctor = doctorRepository.findByUser(doctorUser)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        var patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ApiException(ErrorCode.PATIENT_NOT_FOUND));

        if (patient.getDoctor() == null || !patient.getDoctor().getId().equals(doctor.getId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        OffsetDateTime fromDate = from != null ? from : OffsetDateTime.now().minus(7, ChronoUnit.DAYS);
        OffsetDateTime toDate = to != null ? to : OffsetDateTime.now().plus(7, ChronoUnit.DAYS);

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
}
