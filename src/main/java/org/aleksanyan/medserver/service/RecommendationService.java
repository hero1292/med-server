package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.request.CreateRecommendationRequest;
import org.aleksanyan.medserver.dto.request.UpdateRecommendationRequest;
import org.aleksanyan.medserver.dto.response.RecommendationResponse;
import org.aleksanyan.medserver.dto.response.RecommendationSummaryResponse;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.DoctorProfileRepository;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.RecommendationRepository;
import org.aleksanyan.medserver.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorRepository;
    private final PatientProfileRepository patientRepository;
    private final RecommendationRepository recRepository;
    private final ScheduleService scheduleService;

    @Transactional
    public RecommendationResponse createRecommendation(String doctorEmail, CreateRecommendationRequest request) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (doctorUser.getRole() != Role.DOCTOR)
            throw new ApiException(ErrorCode.ACCESS_DENIED);

        var doctor = doctorRepository.findByUser(doctorUser)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        var patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        Recommendation rec = Recommendation.builder()
                .doctor(doctor)
                .patient(patient)
                .title(request.getTitle())
                .dose(request.getDose())
                .frequencyPerDay(request.getFrequencyPerDay())
                .intervalHours(request.getIntervalHours())
                .durationDays(request.getDurationDays())
                .startAt(request.getStartAt())
                .notes(request.getNotes())
                .build();

        recRepository.save(rec);

        var schedule = scheduleService.generateSchedule(rec);

        return RecommendationResponse.builder()
                .id(rec.getId())
                .title(rec.getTitle())
                .dose(rec.getDose())
                .frequencyPerDay(rec.getFrequencyPerDay())
                .intervalHours(rec.getIntervalHours())
                .durationDays(rec.getDurationDays())
                .startAt(rec.getStartAt())
                .notes(rec.getNotes())
                .doctorId(doctor.getId())
                .patientId(patient.getId())
                .schedule(schedule.stream()
                        .map(s -> ScheduleItemResponse.builder()
                                .id(s.getId())
                                .plannedAt(s.getPlannedAt())
                                .taken(s.getTaken())
                                .build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecommendationSummaryResponse> getRecommendationsByDoctor(String doctorEmail, Long patientId) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (doctorUser.getRole() != Role.DOCTOR) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        var doctor = doctorRepository.findByUser(doctorUser)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        List<Recommendation> recommendations;

        if (patientId != null) {
            var patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
            recommendations = recRepository.findAllByDoctorAndPatient(doctor, patient);
        } else {
            recommendations = recRepository.findAllByDoctor(doctor);
        }

        return recommendations.stream()
                .map(rec -> RecommendationSummaryResponse.builder()
                        .id(rec.getId())
                        .title(rec.getTitle())
                        .dose(rec.getDose())
                        .frequencyPerDay(rec.getFrequencyPerDay())
                        .intervalHours(rec.getIntervalHours())
                        .durationDays(rec.getDurationDays())
                        .startAt(rec.getStartAt())
                        .notes(rec.getNotes())
                        .patientId(rec.getPatient().getId())
                        .patientName(rec.getPatient().getUser().getFullName())
                        .scheduleCount(rec.getScheduleItems() != null ? rec.getScheduleItems().size() : 0)
                        .build())
                .toList();
    }

    @Transactional
    public RecommendationResponse updateRecommendation(String doctorEmail, Long id, UpdateRecommendationRequest req) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (doctorUser.getRole() != Role.DOCTOR)
            throw new ApiException(ErrorCode.ACCESS_DENIED);

        var doctor = doctorRepository.findByUser(doctorUser)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        Recommendation rec = recRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        if (!rec.getDoctor().getId().equals(doctor.getId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }

        rec.setTitle(req.getTitle());
        rec.setDose(req.getDose());
        rec.setFrequencyPerDay(req.getFrequencyPerDay());
        rec.setIntervalHours(req.getIntervalHours());
        rec.setDurationDays(req.getDurationDays());
        rec.setStartAt(req.getStartAt());
        rec.setNotes(req.getNotes());

        recRepository.save(rec);

        scheduleService.deleteFutureItems(rec);

        var newItems = scheduleService.generateSchedule(rec);

        return RecommendationResponse.builder()
                .id(rec.getId())
                .title(rec.getTitle())
                .dose(rec.getDose())
                .frequencyPerDay(rec.getFrequencyPerDay())
                .intervalHours(rec.getIntervalHours())
                .durationDays(rec.getDurationDays())
                .startAt(rec.getStartAt())
                .notes(rec.getNotes())
                .doctorId(rec.getDoctor().getId())
                .patientId(rec.getPatient().getId())
                .schedule(newItems.stream()
                        .map(s -> ScheduleItemResponse.builder()
                                .id(s.getId())
                                .plannedAt(s.getPlannedAt())
                                .taken(s.getTaken())
                                .build())
                        .toList())
                .build();
    }

    @Transactional
    public void deleteRecommendation(String doctorEmail, Long recommendationId) {
        User doctorUser = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (doctorUser.getRole() != Role.DOCTOR) throw new ApiException(ErrorCode.ACCESS_DENIED);

        var doctor = doctorRepository.findByUser(doctorUser)
                .orElseThrow(() -> new ApiException(ErrorCode.PROFILE_NOT_FOUND));

        var rec = recRepository.findByIdAndDoctor(recommendationId, doctor)
                .orElseThrow(() -> new ApiException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        recRepository.delete(rec);
    }
}
