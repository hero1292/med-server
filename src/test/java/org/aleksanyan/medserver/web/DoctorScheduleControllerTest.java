package org.aleksanyan.medserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.service.DoctorScheduleService;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DoctorScheduleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.aleksanyan.medserver.config.SecurityConfig.class,
                org.aleksanyan.medserver.config.JwtAuthFilter.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
class DoctorScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("getPatientSchedule() — возвращает расписание пациента (200 OK)")
    void getPatientSchedule_success() throws Exception {
        var responses = List.of(
                ScheduleItemResponse.builder()
                        .id(1L)
                        .plannedAt(OffsetDateTime.parse("2025-11-01T10:00:00Z"))
                        .taken(true)
                        .note("Принято вовремя")
                        .recommendationTitle("Витамин D")
                        .recommendationDose("1 таблетка")
                        .doctorName("Доктор Хаус")
                        .build()
        );

        Mockito.when(doctorScheduleService.getPatientSchedule(
                eq("doctor@example.com"),
                eq(42L),
                any(),
                any()
        )).thenReturn(responses);

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(get("/api/doctor/patients/42/schedule")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].recommendationTitle").value("Витамин D"))
                .andExpect(jsonPath("$[0].doctorName").value("Доктор Хаус"));
    }
}
