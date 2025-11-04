package org.aleksanyan.medserver.service;

import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.response.PatientShortResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.DoctorProfileRepository;
import org.aleksanyan.medserver.repo.PatientProfileRepository;
import org.aleksanyan.medserver.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DoctorProfileRepository doctorRepository;
    @Mock private PatientProfileRepository patientRepository;

    @InjectMocks
    private DoctorService doctorService;

    private User doctorUser;
    private DoctorProfile doctorProfile;
    private PatientProfile patient1, patient2;

    @BeforeEach
    void setup() {
        doctorUser = User.builder()
                .id(10L)
                .email("doctor@example.com")
                .fullName("Доктор Айболит")
                .role(Role.DOCTOR)
                .build();

        doctorProfile = DoctorProfile.builder()
                .id(1L)
                .user(doctorUser)
                .specialty("Терапевт")
                .build();

        var patientUser1 = User.builder()
                .id(2L)
                .email("ivan@example.com")
                .fullName("Иван Петров")
                .role(Role.PATIENT)
                .build();

        var patientUser2 = User.builder()
                .id(3L)
                .email("maria@example.com")
                .fullName("Мария Смирнова")
                .role(Role.PATIENT)
                .build();

        patient1 = PatientProfile.builder()
                .id(101L)
                .user(patientUser1)
                .doctor(doctorProfile)
                .birthDate(LocalDate.of(1990, 1, 1))
                .phone("+79991234567")
                .address("Москва")
                .build();

        patient2 = PatientProfile.builder()
                .id(102L)
                .user(patientUser2)
                .doctor(doctorProfile)
                .birthDate(LocalDate.of(1985, 5, 5))
                .phone("+79997654321")
                .address("СПб")
                .build();
    }

    @Test
    @DisplayName("getMyPatients() — возвращает список пациентов для DOCTOR")
    void getMyPatients_success() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));
        when(patientRepository.findAllByDoctor(doctorProfile)).thenReturn(List.of(patient1, patient2));

        List<PatientShortResponse> result = doctorService.getMyPatients("doctor@example.com");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("Иван Петров");
        assertThat(result.get(1).getEmail()).isEqualTo("maria@example.com");

        verify(userRepository).findByEmail("doctor@example.com");
        verify(patientRepository).findAllByDoctor(doctorProfile);
    }

    @Test
    @DisplayName("getMyPatients() — выбрасывает USER_NOT_FOUND, если врача нет в БД")
    void getMyPatients_userNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> doctorService.getMyPatients("missing@example.com"),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getMyPatients() — выбрасывает ACCESS_DENIED, если роль не DOCTOR")
    void getMyPatients_notDoctorRole() {
        User patientUser = User.builder()
                .email("patient@example.com")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));

        ApiException ex = catchThrowableOfType(
                () -> doctorService.getMyPatients("patient@example.com"),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("getMyPatients() — выбрасывает PROFILE_NOT_FOUND, если нет профиля врача")
    void getMyPatients_profileNotFound() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> doctorService.getMyPatients("doctor@example.com"),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.PROFILE_NOT_FOUND);
    }

    private void assertApiException(ApiException ex, ErrorCode expected) {
        assertThat(ex)
                .as("Проверка ApiException с кодом " + expected.name())
                .isNotNull();
        assertThat(ex.getCode()).isEqualTo(expected);
        assertThat(ex.getMessage()).isEqualTo(expected.getMessage());
    }
}
