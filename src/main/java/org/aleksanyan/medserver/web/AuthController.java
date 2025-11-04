package org.aleksanyan.medserver.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.dto.request.LoginRequest;
import org.aleksanyan.medserver.dto.request.RefreshTokenRequest;
import org.aleksanyan.medserver.dto.request.RegisterRequest;
import org.aleksanyan.medserver.dto.response.AuthResponse;
import org.aleksanyan.medserver.dto.response.RefreshTokenResponse;
import org.aleksanyan.medserver.dto.response.RegisterResponse;
import org.aleksanyan.medserver.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Регистрация, вход, refresh, logout")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Регистрация нового пользователя (PUBLIC)",
            description = """
                Создаёт нового пользователя (пациента или врача).
                Не требует авторизации. Возвращает данные зарегистрированного пользователя.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Успешная регистрация",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content),
            @ApiResponse(responseCode = "409", description = "Email уже занят", content = @Content)
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(
            summary = "Авторизация (PUBLIC)",
            description = """
                Вход пользователя по email и паролю.
                Возвращает access и refresh токены для последующей авторизации.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешная авторизация",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неверные учётные данные", content = @Content)
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request);
    }

    @Operation(
            summary = "Обновление JWT токенов (PUBLIC)",
            description = """
                Использует refresh-токен (из тела запроса) для выдачи новой пары access/refresh токенов.
                Не требует Bearer-токена в заголовке.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены успешно обновлены",
                    content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный или просроченный refresh-токен", content = @Content),
            @ApiResponse(responseCode = "401", description = "Доступ запрещён", content = @Content)
    })
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @io.swagger.v3.oas.annotations.security.SecurityRequirements
    public RefreshTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    @Operation(
            summary = "Выход из системы (требуется JWT)",
            description = """
                Отзывает refresh-токен пользователя (logout).
                Требует авторизацию по access-токену.
                После выхода refresh-токен становится недействительным.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Успешный выход (токен отозван)"),
            @ApiResponse(responseCode = "401", description = "Неавторизованный запрос (нет JWT)", content = @Content)
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication auth) {
        authService.logout(auth.getName());
    }
}
