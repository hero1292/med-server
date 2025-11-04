package org.aleksanyan.medserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aleksanyan.medserver.dto.request.UpdateDoctorProfileRequest;
import org.aleksanyan.medserver.dto.request.UpdatePatientProfileRequest;
import org.aleksanyan.medserver.dto.response.DoctorProfileResponse;
import org.aleksanyan.medserver.dto.response.PatientProfileResponse;
import org.aleksanyan.medserver.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.aleksanyan.medserver.config.SecurityConfig.class,
                org.aleksanyan.medserver.config.JwtAuthFilter.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/profile/me — возвращает профиль пациента")
    void getProfile_patient_success() throws Exception {
        var response = PatientProfileResponse.builder()
                .id(1L)
                .fullName("Иван Иванов")
                .email("patient@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .phone("+79995556677")
                .address("Москва")
                .doctorName("Доктор Айболит")
                .build();

        Mockito.when(profileService.getMyProfile("patient@example.com")).thenReturn(response);

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("patient@example.com");

        mockMvc.perform(get("/api/profile/me")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Иван Иванов"))
                .andExpect(jsonPath("$.doctorName").value("Доктор Айболит"));
    }

    @Test
    @DisplayName("PUT /api/profile/me/patient — обновляет профиль пациента (200 OK)")
    void updatePatientProfile_success() throws Exception {
        var response = PatientProfileResponse.builder()
                .id(2L)
                .fullName("Иван Петров")
                .email("patient@example.com")
                .address("СПб")
                .build();

        Mockito.when(profileService.updatePatientProfile(eq("patient@example.com"), any()))
                .thenReturn(response);

        var request = new UpdatePatientProfileRequest();
        request.setFullName("Иван Петров");
        request.setAddress("СПб");

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("patient@example.com");

        mockMvc.perform(put("/api/profile/me/patient")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Иван Петров"))
                .andExpect(jsonPath("$.address").value("СПб"));
    }

    @Test
    @DisplayName("PUT /api/profile/me/doctor — обновляет профиль врача (200 OK)")
    void updateDoctorProfile_success() throws Exception {
        var response = DoctorProfileResponse.builder()
                .id(10L)
                .fullName("Доктор Хаус")
                .email("doctor@example.com")
                .specialty("Кардиолог")
                .phone("+79991112233")
                .build();

        Mockito.when(profileService.updateDoctorProfile(eq("doctor@example.com"), any()))
                .thenReturn(response);

        var request = new UpdateDoctorProfileRequest();
        request.setFullName("Доктор Хаус");
        request.setSpecialty("Кардиолог");
        request.setPhone("+79991112233");

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(put("/api/profile/me/doctor")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Доктор Хаус"))
                .andExpect(jsonPath("$.specialty").value("Кардиолог"))
                .andExpect(jsonPath("$.phone").value("+79991112233"));
    }
}
