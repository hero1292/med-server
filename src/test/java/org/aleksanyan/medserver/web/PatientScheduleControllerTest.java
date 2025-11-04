package org.aleksanyan.medserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aleksanyan.medserver.dto.request.TakeMedicineRequest;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.service.ScheduleService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PatientScheduleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.aleksanyan.medserver.config.SecurityConfig.class,
                org.aleksanyan.medserver.config.JwtAuthFilter.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
class PatientScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/patient/schedule — возвращает список расписания пациента (200 OK)")
    void getSchedule_success() throws Exception {
        var items = List.of(
                ScheduleItemResponse.builder()
                        .id(1L)
                        .plannedAt(OffsetDateTime.parse("2025-11-04T10:00:00Z"))
                        .taken(false)
                        .recommendationTitle("Витамин D")
                        .recommendationDose("1 таблетка")
                        .doctorName("Доктор Хаус")
                        .build()
        );

        Mockito.when(scheduleService.getPatientSchedule(eq("patient@example.com"), any(), any()))
                .thenReturn(items);

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("patient@example.com");

        mockMvc.perform(get("/api/patient/schedule")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recommendationTitle").value("Витамин D"))
                .andExpect(jsonPath("$[0].doctorName").value("Доктор Хаус"));
    }

    @Test
    @DisplayName("PATCH /api/patient/schedule/{itemId}/taken — отмечает приём лекарства (200 OK)")
    void markAsTaken_success() throws Exception {
        var response = ScheduleItemResponse.builder()
                .id(123L)
                .taken(true)
                .note("После еды")
                .recommendationTitle("Аскорбинка")
                .doctorName("Доктор Айболит")
                .build();

        Mockito.when(scheduleService.markItemAsTaken(eq("patient@example.com"), eq(123L), any()))
                .thenReturn(response);

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("patient@example.com");

        var request = new TakeMedicineRequest();
        request.setNote("После еды");

        mockMvc.perform(patch("/api/patient/schedule/123/taken")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.note").value("После еды"))
                .andExpect(jsonPath("$.recommendationTitle").value("Аскорбинка"))
                .andExpect(jsonPath("$.doctorName").value("Доктор Айболит"));
    }
}
