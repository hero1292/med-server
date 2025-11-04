package org.aleksanyan.medserver.service;

import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.request.CreateRecommendationRequest;
import org.aleksanyan.medserver.dto.request.UpdateRecommendationRequest;
import org.aleksanyan.medserver.dto.response.RecommendationResponse;
import org.aleksanyan.medserver.dto.response.RecommendationSummaryResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DoctorProfileRepository doctorRepository;
    @Mock private PatientProfileRepository patientRepository;
    @Mock private RecommendationRepository recRepository;
    @Mock private ScheduleService scheduleService;

    @InjectMocks
    private RecommendationService recommendationService;

    private User doctorUser;
    private User patientUser;
    private DoctorProfile doctorProfile;
    private PatientProfile patientProfile;
    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        doctorUser = User.builder()
                .id(1L)
                .email("doctor@example.com")
                .role(Role.DOCTOR)
                .fullName("Доктор Айболит")
                .build();

        patientUser = User.builder()
                .id(2L)
                .email("patient@example.com")
                .role(Role.PATIENT)
                .fullName("Иван Иванов")
                .build();

        doctorProfile = DoctorProfile.builder()
                .id(10L)
                .user(doctorUser)
                .specialty("Терапевт")
                .build();

        patientProfile = PatientProfile.builder()
                .id(20L)
                .user(patientUser)
                .doctor(doctorProfile)
                .build();

        recommendation = Recommendation.builder()
                .id(100L)
                .doctor(doctorProfile)
                .patient(patientProfile)
                .title("Витамин C")
                .dose("500мг")
                .frequencyPerDay(2)
                .intervalHours(12)
                .durationDays(7)
                .startAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createRecommendation() — успешно создаёт рекомендацию")
    void createRecommendation_success() {
        CreateRecommendationRequest request = new CreateRecommendationRequest();
        request.setPatientId(20L);
        request.setTitle("Витамин C");
        request.setDose("500мг");
        request.setFrequencyPerDay(2);
        request.setIntervalHours(12);
        request.setDurationDays(7);
        request.setStartAt(OffsetDateTime.now());

        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(patientRepository.findById(20L)).thenReturn(Optional.of(patientProfile));
        when(recRepository.save(any())).thenReturn(recommendation);
        when(scheduleService.generateSchedule(any())).thenReturn(List.of());

        RecommendationResponse response =
                recommendationService.createRecommendation("doctor@example.com", request);

        assertThat(response.getTitle()).isEqualTo("Витамин C");
        assertThat(response.getDoctorId()).isEqualTo(10L);
        assertThat(response.getPatientId()).isEqualTo(20L);
        verify(recRepository).save(any(Recommendation.class));
        verify(scheduleService).generateSchedule(any(Recommendation.class));
    }

    @Test
    @DisplayName("getRecommendationsByDoctor() — возвращает список рекомендаций")
    void getRecommendationsByDoctor_success() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(recRepository.findAllByDoctor(doctorProfile)).thenReturn(List.of(recommendation));

        var result = recommendationService.getRecommendationsByDoctor("doctor@example.com", null);

        assertThat(result).hasSize(1);
        RecommendationSummaryResponse summary = result.get(0);
        assertThat(summary.getTitle()).isEqualTo("Витамин C");
    }

    @Test
    @DisplayName("updateRecommendation() — успешно обновляет рекомендацию")
    void updateRecommendation_success() {
        UpdateRecommendationRequest request = new UpdateRecommendationRequest();
        request.setTitle("Витамин D");
        request.setDose("1000мг");
        request.setFrequencyPerDay(1);
        request.setIntervalHours(24);
        request.setDurationDays(5);
        request.setStartAt(OffsetDateTime.now());

        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(recRepository.findById(100L)).thenReturn(Optional.of(recommendation));
        when(scheduleService.generateSchedule(recommendation)).thenReturn(List.of());
        when(recRepository.save(recommendation)).thenReturn(recommendation);

        RecommendationResponse response =
                recommendationService.updateRecommendation("doctor@example.com", 100L, request);

        assertThat(response.getTitle()).isEqualTo("Витамин D");
        verify(scheduleService).deleteFutureItems(recommendation);
        verify(scheduleService).generateSchedule(recommendation);
    }

    @Test
    @DisplayName("deleteRecommendation() — успешно удаляет рекомендацию")
    void deleteRecommendation_success() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(recRepository.findByIdAndDoctor(100L, doctorProfile)).thenReturn(Optional.of(recommendation));

        recommendationService.deleteRecommendation("doctor@example.com", 100L);

        verify(recRepository).delete(recommendation);
    }

    @Test
    @DisplayName("createRecommendation() — выбрасывает ACCESS_DENIED, если не DOCTOR")
    void createRecommendation_accessDenied() {
        doctorUser.setRole(Role.PATIENT);
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));

        ApiException ex = catchThrowableOfType(
                () -> recommendationService.createRecommendation("doctor@example.com", new CreateRecommendationRequest()),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("updateRecommendation() — выбрасывает ACCESS_DENIED, если врач не владелец")
    void updateRecommendation_notOwner() {
        DoctorProfile anotherDoctor = DoctorProfile.builder().id(999L).build();
        recommendation.setDoctor(anotherDoctor);

        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(recRepository.findById(100L)).thenReturn(Optional.of(recommendation));

        ApiException ex = catchThrowableOfType(
                () -> recommendationService.updateRecommendation("doctor@example.com", 100L, new UpdateRecommendationRequest()),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    private void assertApiException(ApiException ex, ErrorCode expected) {
        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(expected);
        assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    }
}
