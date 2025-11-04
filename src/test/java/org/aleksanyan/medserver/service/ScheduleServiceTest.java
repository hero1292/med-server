package org.aleksanyan.medserver.service;

import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.request.TakeMedicineRequest;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
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
class ScheduleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PatientProfileRepository patientRepository;
    @Mock private ScheduleItemRepository scheduleRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private User patientUser;
    private PatientProfile patientProfile;
    private DoctorProfile doctorProfile;
    private ScheduleItem scheduleItem;
    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        doctorProfile = DoctorProfile.builder()
                .id(1L)
                .user(User.builder()
                        .email("doc@example.com")
                        .fullName("Доктор Айболит")
                        .role(Role.DOCTOR)
                        .build())
                .build();

        patientUser = User.builder()
                .id(10L)
                .email("patient@example.com")
                .fullName("Пациент Иванов")
                .role(Role.PATIENT)
                .build();

        patientProfile = PatientProfile.builder()
                .id(5L)
                .user(patientUser)
                .doctor(doctorProfile)
                .build();

        recommendation = Recommendation.builder()
                .id(100L)
                .doctor(doctorProfile)
                .patient(patientProfile)
                .title("Витамин C")
                .dose("500 мг")
                .durationDays(3)
                .frequencyPerDay(2)
                .startAt(OffsetDateTime.now())
                .build();

        scheduleItem = ScheduleItem.builder()
                .id(1L)
                .patient(patientProfile)
                .recommendation(recommendation)
                .plannedAt(OffsetDateTime.now())
                .taken(false)
                .build();
    }

    @Test
    @DisplayName("getPatientSchedule() — возвращает список расписания для PATIENT")
    void getPatientSchedule_success() {
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));
        when(scheduleRepository.findAllByPatientAndPlannedAtBetweenOrderByPlannedAtAsc(any(), any(), any()))
                .thenReturn(List.of(scheduleItem));

        var result = scheduleService.getPatientSchedule("patient@example.com", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecommendationTitle()).isEqualTo("Витамин C");
        assertThat(result.get(0).getDoctorName()).isEqualTo("Доктор Айболит");
    }

    @Test
    @DisplayName("markItemAsTaken() — отмечает расписание как принято и сохраняет")
    void markItemAsTaken_success() {
        scheduleItem.setTaken(false);
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleItem));

        var request = new TakeMedicineRequest();
        request.setNote("После еды");

        var result = scheduleService.markItemAsTaken("patient@example.com", 1L, request);

        assertThat(result.getTaken()).isTrue();
        assertThat(result.getNote()).isEqualTo("После еды");
        verify(scheduleRepository).save(scheduleItem);
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает USER_NOT_FOUND, если пользователь не найден")
    void getPatientSchedule_userNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> scheduleService.getPatientSchedule("unknown@example.com", null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getPatientSchedule() — выбрасывает ACCESS_DENIED, если роль не PATIENT")
    void getPatientSchedule_accessDenied() {
        var doctorUser = User.builder().email("doc@example.com").role(Role.DOCTOR).build();
        when(userRepository.findByEmail("doc@example.com")).thenReturn(Optional.of(doctorUser));

        ApiException ex = catchThrowableOfType(
                () -> scheduleService.getPatientSchedule("doc@example.com", null, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("markItemAsTaken() — выбрасывает ACCESS_DENIED, если элемент чужой")
    void markItemAsTaken_accessDenied() {
        PatientProfile anotherPatient = PatientProfile.builder()
                .id(99L)
                .user(User.builder().email("other@example.com").role(Role.PATIENT).build())
                .build();

        scheduleItem.setPatient(anotherPatient);

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(scheduleItem));

        ApiException ex = catchThrowableOfType(
                () -> scheduleService.markItemAsTaken("patient@example.com", 1L, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("markItemAsTaken() — выбрасывает SCHEDULE_ITEM_NOT_FOUND, если нет элемента")
    void markItemAsTaken_itemNotFound() {
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> scheduleService.markItemAsTaken("patient@example.com", 1L, null),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.SCHEDULE_ITEM_NOT_FOUND);
    }

    private void assertApiException(ApiException ex, ErrorCode expected) {
        assertThat(ex).isNotNull();
        assertThat(ex.getCode()).isEqualTo(expected);
        assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    }
}
