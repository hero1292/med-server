package org.aleksanyan.medserver.service;

import org.aleksanyan.medserver.domain.*;
import org.aleksanyan.medserver.dto.request.UpdateDoctorProfileRequest;
import org.aleksanyan.medserver.dto.request.UpdatePatientProfileRequest;
import org.aleksanyan.medserver.dto.response.DoctorProfileResponse;
import org.aleksanyan.medserver.dto.response.PatientProfileResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DoctorProfileRepository doctorRepository;
    @Mock private PatientProfileRepository patientRepository;

    @InjectMocks
    private ProfileService profileService;

    private User doctorUser;
    private User patientUser;
    private DoctorProfile doctorProfile;
    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        doctorUser = User.builder()
                .id(1L)
                .email("doctor@example.com")
                .fullName("Доктор Айболит")
                .role(Role.DOCTOR)
                .build();

        doctorProfile = DoctorProfile.builder()
                .id(10L)
                .user(doctorUser)
                .specialty("Терапевт")
                .phone("+79991234567")
                .build();

        patientUser = User.builder()
                .id(2L)
                .email("patient@example.com")
                .fullName("Иван Иванов")
                .role(Role.PATIENT)
                .build();

        patientProfile = PatientProfile.builder()
                .id(20L)
                .user(patientUser)
                .phone("+79995556677")
                .address("Москва")
                .birthDate(LocalDate.of(1990, 1, 1))
                .doctor(doctorProfile)
                .build();
    }

    @Test
    @DisplayName("getMyProfile() — возвращает профиль врача")
    void getMyProfile_doctor_success() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));

        Object result = profileService.getMyProfile("doctor@example.com");

        assertThat(result).isInstanceOf(DoctorProfileResponse.class);
        DoctorProfileResponse response = (DoctorProfileResponse) result;
        assertThat(response.getSpecialty()).isEqualTo("Терапевт");
    }

    @Test
    @DisplayName("getMyProfile() — возвращает профиль пациента")
    void getMyProfile_patient_success() {
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));

        Object result = profileService.getMyProfile("patient@example.com");

        assertThat(result).isInstanceOf(PatientProfileResponse.class);
        PatientProfileResponse response = (PatientProfileResponse) result;
        assertThat(response.getDoctorName()).isEqualTo("Доктор Айболит");
    }

    @Test
    @DisplayName("updateDoctorProfile() — обновляет профиль врача")
    void updateDoctorProfile_success() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUser(doctorUser)).thenReturn(Optional.of(doctorProfile));

        UpdateDoctorProfileRequest request = new UpdateDoctorProfileRequest();
        request.setFullName("Доктор Хаус");
        request.setSpecialty("Кардиолог");
        request.setPhone("+79990001122");

        DoctorProfileResponse response = profileService.updateDoctorProfile("doctor@example.com", request);

        assertThat(response.getFullName()).isEqualTo("Доктор Хаус");
        assertThat(response.getSpecialty()).isEqualTo("Кардиолог");

        verify(userRepository).save(doctorUser);
        verify(doctorRepository).save(doctorProfile);
    }

    @Test
    @DisplayName("updatePatientProfile() — обновляет профиль пациента")
    void updatePatientProfile_success() {
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUser(patientUser)).thenReturn(Optional.of(patientProfile));
        when(doctorRepository.findById(10L)).thenReturn(Optional.of(doctorProfile));

        UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
        request.setFullName("Иван Петров");
        request.setPhone("+79997778899");
        request.setAddress("СПб");
        request.setDoctorId(10L);

        PatientProfileResponse response = profileService.updatePatientProfile("patient@example.com", request);

        assertThat(response.getFullName()).isEqualTo("Иван Петров");
        assertThat(response.getAddress()).isEqualTo("СПб");
        verify(patientRepository).save(patientProfile);
    }

    @Test
    @DisplayName("getMyProfile() — выбрасывает USER_NOT_FOUND, если email не найден")
    void getMyProfile_userNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ApiException ex = catchThrowableOfType(
                () -> profileService.getMyProfile("missing@example.com"),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("updateDoctorProfile() — выбрасывает ACCESS_DENIED, если не DOCTOR")
    void updateDoctorProfile_accessDenied() {
        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(patientUser));

        ApiException ex = catchThrowableOfType(
                () -> profileService.updateDoctorProfile("patient@example.com", new UpdateDoctorProfileRequest()),
                ApiException.class
        );

        assertApiException(ex, ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("updatePatientProfile() — выбрасывает ACCESS_DENIED, если не PATIENT")
    void updatePatientProfile_accessDenied() {
        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.of(doctorUser));

        ApiException ex = catchThrowableOfType(
                () -> profileService.updatePatientProfile("doctor@example.com", new UpdatePatientProfileRequest()),
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
