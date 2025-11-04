package org.aleksanyan.medserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aleksanyan.medserver.dto.request.CreateRecommendationRequest;
import org.aleksanyan.medserver.dto.request.UpdateRecommendationRequest;
import org.aleksanyan.medserver.dto.response.RecommendationResponse;
import org.aleksanyan.medserver.dto.response.RecommendationSummaryResponse;
import org.aleksanyan.medserver.service.RecommendationService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecommendationController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.aleksanyan.medserver.config.SecurityConfig.class,
                org.aleksanyan.medserver.config.JwtAuthFilter.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/doctor/recommendations — создаёт рекомендацию (201 Created)")
    void createRecommendation_success() throws Exception {
        var response = RecommendationResponse.builder()
                .id(101L)
                .title("Витамин C")
                .dose("500мг")
                .doctorId(10L)
                .patientId(20L)
                .build();

        Mockito.when(recommendationService.createRecommendation(eq("doctor@example.com"), any()))
                .thenReturn(response);

        var request = new CreateRecommendationRequest();
        request.setPatientId(20L);
        request.setTitle("Витамин C");
        request.setDose("500мг");
        request.setFrequencyPerDay(2);
        request.setIntervalHours(12);
        request.setDurationDays(7);
        request.setStartAt(OffsetDateTime.now());

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(post("/api/doctor/recommendations")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Витамин C"))
                .andExpect(jsonPath("$.doctorId").value(10))
                .andExpect(jsonPath("$.patientId").value(20));
    }

    @Test
    @DisplayName("GET /api/doctor/recommendations — возвращает список рекомендаций (200 OK)")
    void getRecommendations_success() throws Exception {
        var summary = RecommendationSummaryResponse.builder()
                .id(1L)
                .title("Аскорбинка")
                .dose("500мг")
                .patientName("Иван Иванов")
                .build();

        Mockito.when(recommendationService.getRecommendationsByDoctor(eq("doctor@example.com"), any()))
                .thenReturn(List.of(summary));

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(get("/api/doctor/recommendations")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Аскорбинка"))
                .andExpect(jsonPath("$[0].patientName").value("Иван Иванов"));
    }

    @Test
    @DisplayName("PUT /api/doctor/recommendations/{id} — обновляет рекомендацию (200 OK)")
    void updateRecommendation_success() throws Exception {
        var response = RecommendationResponse.builder()
                .id(100L)
                .title("Витамин D")
                .dose("1000мг")
                .build();

        Mockito.when(recommendationService.updateRecommendation(eq("doctor@example.com"), eq(100L), any()))
                .thenReturn(response);

        var request = new UpdateRecommendationRequest();
        request.setTitle("Витамин D");
        request.setDose("1000мг");
        request.setFrequencyPerDay(1);
        request.setIntervalHours(24);
        request.setDurationDays(5);
        request.setStartAt(OffsetDateTime.now());

        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(put("/api/doctor/recommendations/100")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Витамин D"))
                .andExpect(jsonPath("$.dose").value("1000мг"));
    }

    @Test
    @DisplayName("DELETE /api/doctor/recommendations/{id} — удаляет рекомендацию (204 No Content)")
    void deleteRecommendation_success() throws Exception {
        Authentication auth = Mockito.mock(Authentication.class);
        Mockito.when(auth.getName()).thenReturn("doctor@example.com");

        mockMvc.perform(delete("/api/doctor/recommendations/100")
                        .principal(auth))
                .andExpect(status().isNoContent());

        verify(recommendationService).deleteRecommendation("doctor@example.com", 100L);
    }
}
