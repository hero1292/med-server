package org.aleksanyan.medserver.service;

import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.config.JwtService;
import org.aleksanyan.medserver.domain.Role;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final AuthenticationManager authManager;
    private final ProfileService profileService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.INVALID_ROLE);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .build();

        userRepository.save(user);

        profileService.createProfileForUser(user);

        return RegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .message("Регистрация прошла успешно")
                .build();
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        try {
            var auth = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
            authManager.authenticate(auth);
        } catch (AuthenticationException ex) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        String access = jwtService.generateAccess(user.getEmail(), Map.of("role", user.getRole().name()));
        tokenService.issueRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(access)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .message("Аутентификация успешна")
                .build();
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        var token = tokenService.validateRefreshToken(request.getRefreshToken());
        User user = token.getUser();

        String newAccess = jwtService.generateAccess(user.getEmail(), Map.of("role", user.getRole().name()));
        String newRefresh = tokenService.issueRefreshToken(user);

        token.setRevoked(true);
        return RefreshTokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .message("Токен успешно обновлён")
                .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        tokenService.revokeTokens(user);
    }
}
