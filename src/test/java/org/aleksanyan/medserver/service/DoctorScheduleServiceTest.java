package org.aleksanyan.medserver.service;

import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.DoctorProfileRepository;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.ScheduleItemRepository;
import org.aleksanyan.medserver.repo.UserRepository;
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
class DoctorScheduleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DoctorProfileRepository doctorRepository;
    @Mock private PatientProfileRepository patientRepository;
    @Mock private ScheduleItemRepository scheduleRepository;

    @InjectMocks
    private DoctorScheduleService doctorScheduleService;

    private User doctorUser;
    private DoctorProfile doctorProfile;
    private PatientProfile patientProfile;
    private ScheduleItem item1;
    private ScheduleItem item2;
    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        doctorUser = User.builder()
                .id(10L)
                .email("doctor@example.com")
                .fullName("Доктор Хаус")
                .role(Role.DOCTOR)
                .build();

        doctorProfile = DoctorProfile.builder()
                .id(1L)
                .user(doctorUser)
                .build();

        User patientUser = User.builder()
                .id(20L)
                .email("patient@example.com")
                .fullName("Пациент Иванов")
                .role(Role.PATIENT)
                .build();

        patientProfile = PatientProfile.builder()
                .id(2L)
                .user(patientUser)
                .doctor(doctorProfile)
                .build();

        recommendation = Recommendation.builder()
                .id(100L)
                .doctor(doctorProfile)
                .title("Витамин D")
                .dose("1 таблетка в день")
                .build();

        item1 = ScheduleItem.builder()
                .id(1L)
                .patient(patientProfile)
                .plannedAt(OffsetDateTime.now().minusDays(1))
                .taken(true)
                .takenAt(OffsetDateTime.now().minusDays(1))
                .note("Принято вовремя")
                .recommendation(recommendation)
                .build();

        item2 = ScheduleItem.builder()
                .id(2L)
                .patient(patientProfile)
                .plannedAt(OffsetDateTime.now().plusDays(1))
                .taken(false)
                .recommendation(recommendation)
                .build();
    }

    @Test
    @DisplayName("getPatientSchedule() — возвращает расписание пациента для DOCTOR")
    void getPatientSchedule_success() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patientProfile));
        when(scheduleRepository.findAllByPatientAndPlannedAtBetweenOrderByPlannedAtAsc(any(), any(), any()))
                .thenReturn(List.of(item1, item2));

        List<ScheduleItemResponse> result = doctorScheduleService.getPatientSchedule(
                "doctor@example.com",
                2L,
                null,
                null
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRecommendationTitle()).isEqualTo("Витамин D");
        assertThat(result.get(0).getDoctorName()).isEqualTo("Доктор Хаус");

        verify(scheduleRepository).findAllByPatientAndPlannedAtBetweenOrderByPlannedAtAsc(any(), any(), any());
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает DOCTOR_NOT_FOUND, если врач не найден")
    void getPatientSchedule_doctorNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> doctorScheduleService.getPatientSchedule("missing@example.com", 2L, null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.DOCTOR_NOT_FOUND);
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает ACCESS_DENIED, если роль не DOCTOR")
    void getPatientSchedule_notDoctorRole() {
        User user = User.builder()
                .email("patient@example.com")
                .role(Role.PATIENT)
                .build();
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));

        ApiException ex = catchThrowableOfType(
                () -> doctorScheduleService.getPatientSchedule("patient@example.com", 2L, null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает PROFILE_NOT_FOUND, если профиль врача отсутствует")
    void getPatientSchedule_profileNotFound() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> doctorScheduleService.getPatientSchedule("doctor@example.com", 2L, null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает PATIENT_NOT_FOUND, если пациент не найден")
    void getPatientSchedule_patientNotFound() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(patientRepository.findById(2L)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> doctorScheduleService.getPatientSchedule("doctor@example.com", 2L, null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.PATIENT_NOT_FOUND);
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает ACCESS_DENIED, если пациент не принадлежит врачу")
    void getPatientSchedule_accessDenied_notHisPatient() {
        DoctorProfile anotherDoctor = DoctorProfile.builder()
                .id(99L)
                .user(User.builder().email("another@doc.com").role(Role.DOCTOR).build())
                .build();

        patientProfile.setDoctor(anotherDoctor);

        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patientProfile));

        ApiException ex = catchThrowableOfType(
                () -> doctorScheduleService.getPatientSchedule("doctor@example.com", 2L, null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    private void assertApiException(ApiException ex, ErrorCode expected) {
        assertThat(ex)
                .as("Проверка ApiException с кодом " + expected.name())
                .isNotNull();
        assertThat(ex.getCode()).isEqualTo(expected);
        assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    }
}
