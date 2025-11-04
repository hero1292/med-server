package org.aleksanyan.medserver.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aleksanyan.medserver.dto.request.LoginRequest;
import org.aleksanyan.medserver.dto.request.RefreshTokenRequest;
import org.aleksanyan.medserver.dto.request.RegisterRequest;
import org.aleksanyan.medserver.dto.response.AuthResponse;
import org.aleksanyan.medserver.dto.response.RefreshTokenResponse;
import org.aleksanyan.medserver.dto.response.RegisterResponse;
import org.aleksanyan.medserver.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                org.aleksanyan.medserver.config.SecurityConfig.class,
                org.aleksanyan.medserver.config.JwtAuthFilter.class
        })
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("register() — успешная регистрация возвращает 201 и тело RegisterResponse")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("password123");
        req.setFullName("Иван Иванов");
        req.setRole("PATIENT");

        RegisterResponse resp = RegisterResponse.builder()
                .id(1L)
                .email(req.getEmail())
                .fullName(req.getFullName())
                .role(req.getRole())
                .message("Регистрация прошла успешно")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(req.getEmail()))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.message").value("Регистрация прошла успешно"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("register() — ошибка валидации возвращает 400")
    void register_validationError() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("invalid-email");
        req.setPassword("123");
        req.setFullName("");
        req.setRole("INVALID_ROLE");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login() — успешная аутентификация возвращает 200 и AuthResponse")
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("securepass");

        AuthResponse resp = AuthResponse.builder()
                .accessToken("jwt-access-token")
                .tokenType("Bearer")
                .role("DOCTOR")
                .message("Аутентификация успешна")
                .build();

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.message").value("Аутентификация успешна"));

        verify(authService).authenticate(any(LoginRequest.class));
    }

    @Test
    @DisplayName("login() — некорректные данные запроса возвращают 400")
    void login_validationError() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("");
        req.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).authenticate(any());
    }

    @Test
    @DisplayName("refresh() — успешное обновление токенов возвращает 200 и RefreshTokenResponse")
    void refresh_success() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh123");

        RefreshTokenResponse resp = RefreshTokenResponse.builder()
                .accessToken("newAccess")
                .refreshToken("newRefresh")
                .tokenType("Bearer")
                .message("Токен успешно обновлён")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccess"))
                .andExpect(jsonPath("$.refreshToken").value("newRefresh"))
                .andExpect(jsonPath("$.message").value("Токен успешно обновлён"));

        verify(authService).refreshToken(any(RefreshTokenRequest.class));
    }

    @Test
    @DisplayName("refresh() — отсутствует refreshToken возвращает 400")
    void refresh_validationError() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).refreshToken(any());
    }

    @Test
    @DisplayName("logout() — успешный выход вызывает logout() в сервисе и возвращает 204")
    void logout_success() throws Exception {
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("user@example.com");

        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .principal(mockAuth))
                .andExpect(status().isNoContent());

        verify(authService).logout("user@example.com");
    }
}
