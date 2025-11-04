package org.aleksanyan.medserver.service;

import org.aleksanyan.medserver.config.JwtService;
import org.aleksanyan.medserver.domain.Role;
import org.aleksanyan.medserver.domain.Token;
import org.aleksanyan.medserver.domain.User;
import org.aleksanyan.medserver.dto.request.LoginRequest;
import org.aleksanyan.medserver.dto.request.RefreshTokenRequest;
import org.aleksanyan.medserver.dto.request.RegisterRequest;
import org.aleksanyan.medserver.dto.response.AuthResponse;
import org.aleksanyan.medserver.dto.response.RefreshTokenResponse;
import org.aleksanyan.medserver.dto.response.RegisterResponse;
import org.aleksanyan.medserver.exception.ApiException;
import org.aleksanyan.medserver.exception.ErrorCode;
import org.aleksanyan.medserver.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private AuthenticationManager authManager;
    @Mock private ProfileService profileService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("secure123");
        registerRequest.setFullName("Иван Иванов");
        registerRequest.setRole("PATIENT");
    }

    @Test
    @DisplayName("register() — успешная регистрация сохраняет пользователя и возвращает RegisterResponse")
    void register_success() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secure123")).thenReturn("encodedPass");

        User savedUser = User.builder()
                .id(1L)
                .email("new@example.com")
                .fullName("Иван Иванов")
                .passwordHash("encodedPass")
                .role(Role.PATIENT)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(registerRequest);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getRole()).isEqualTo("PATIENT");
        assertThat(response.getMessage()).contains("успешно");

        verify(userRepository).save(any(User.class));
        verify(profileService).createProfileForUser(any(User.class));
    }

    @Test
    @DisplayName("register() — если email уже существует, выбрасывает EMAIL_ALREADY_EXISTS")
    void register_emailAlreadyExists() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("register() — при неверной роли выбрасывает INVALID_ROLE")
    void register_invalidRole() {
        registerRequest.setRole("UNKNOWN");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_ROLE);
    }

    @Test
    @DisplayName("authenticate() — успешная аутентификация возвращает AuthResponse")
    void authenticate_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("secret");

        User user = User.builder()
                .email(req.getEmail())
                .role(Role.DOCTOR)
                .build();

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccess(eq(req.getEmail()), anyMap())).thenReturn("jwt-access");
        when(tokenService.issueRefreshToken(user)).thenReturn("refresh123");

        AuthResponse resp = authService.authenticate(req);

        assertThat(resp.getAccessToken()).isEqualTo("jwt-access");
        assertThat(resp.getRole()).isEqualTo("DOCTOR");
        assertThat(resp.getMessage()).contains("успешна");

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenService).issueRefreshToken(user);
    }

    @Test
    @DisplayName("authenticate() — при неверных данных выбрасывает INVALID_CREDENTIALS")
    void authenticate_invalidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@example.com");
        req.setPassword("wrong");

        doThrow(new BadCredentialsException("bad"))
                .when(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.authenticate(req))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("authenticate() — если пользователь не найден, выбрасывает INVALID_CREDENTIALS")
    void authenticate_userNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@example.com");
        req.setPassword("password");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(req))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("refreshToken() — успешное обновление токена возвращает RefreshTokenResponse")
    void refreshToken_success() {
        User user = User.builder()
                .email("user@example.com")
                .role(Role.PATIENT)
                .build();

        Token oldToken = Token.builder()
                .user(user)
                .refreshToken("oldToken")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("oldToken");

        when(tokenService.validateRefreshToken("oldToken")).thenReturn(oldToken);
        when(jwtService.generateAccess(eq("user@example.com"), anyMap())).thenReturn("newAccess");
        when(tokenService.issueRefreshToken(user)).thenReturn("newRefresh");

        RefreshTokenResponse resp = authService.refreshToken(req);

        assertThat(resp.getAccessToken()).isEqualTo("newAccess");
        assertThat(resp.getRefreshToken()).isEqualTo("newRefresh");
        assertThat(resp.getMessage()).contains("обновлён");

        assertThat(oldToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("logout() — успешное выполнение отзывает токены пользователя")
    void logout_success() {
        User user = User.builder().email("user@example.com").build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        authService.logout("user@example.com");

        verify(tokenService).revokeTokens(user);
    }

    @Test
    @DisplayName("logout() — если пользователь не найден, выбрасывает USER_NOT_FOUND")
    void logout_userNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("missing@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
