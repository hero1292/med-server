package org.aleksanyan.medserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aleksanyan.medserver.dto.response.PatientShortResponse;
import org.aleksanyan.medserver.service.DoctorService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DoctorController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.aleksanyan.medserver.config.SecurityConfig.class,
                org.aleksanyan.medserver.config.JwtAuthFilter.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("getMyPatients() — возвращает список пациентов врача (200 OK)")
    void getMyPatients_success() throws Exception {
        var patients = List.of(
                PatientShortResponse.builder()
                        .id(1L)
                        .fullName("Иван Петров")
                        .email("ivan@example.com")
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .phone("+79991234567")
                        .address("Москва, ул. Пушкина")
                        .build(),
                PatientShortResponse.builder()
                        .id(2L)
                        .fullName("Мария Смирнова")
                        .email("maria@example.com")
                        .birthDate(LocalDate.of(1985, 5, 5))
                        .phone("+79997654321")
                        .address("СПб, Невский пр.")
                        .build()
        );

        Mockito.when(doctorService.getMyPatients(anyString())).thenReturn(patients);

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(get("/api/doctor/patients")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Иван Петров"))
                .andExpect(jsonPath("$[1].email").value("maria@example.com"))
                .andExpect(jsonPath("$[0].id").value(1));

        verify(doctorService).getMyPatients("doctor@example.com");
    }
}
