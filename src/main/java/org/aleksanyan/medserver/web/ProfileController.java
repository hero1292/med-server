package org.aleksanyan.medserver.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.dto.request.UpdateDoctorProfileRequest;
import org.aleksanyan.medserver.dto.request.UpdatePatientProfileRequest;
import org.aleksanyan.medserver.dto.response.DoctorProfileResponse;
import org.aleksanyan.medserver.dto.response.PatientProfileResponse;
import org.aleksanyan.medserver.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Работа с профилем пользователя (пациент / врач)")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
            summary = "Получить свой профиль",
            description = """
                Возвращает профиль текущего пользователя.
                Если пользователь — пациент, возвращается `PatientProfileResponse`;
                если врач — `DoctorProfileResponse`.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль успешно получен",
                    content = {
                            @Content(schema = @Schema(implementation = PatientProfileResponse.class)),
                            @Content(schema = @Schema(implementation = DoctorProfileResponse.class))
                    }),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT токена)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Профиль не найден", content = @Content)
    })
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public Object getProfile(Authentication auth) {
        return profileService.getMyProfile(auth.getName());
    }

    @Operation(
            summary = "Обновить профиль пациента",
            description = """
                Позволяет пациенту обновить личные данные:
                ФИО, телефон, адрес и дату рождения.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль пациента успешно обновлён",
                    content = @Content(schema = @Schema(implementation = PatientProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не PATIENT)", content = @Content)
    })
    @PutMapping("/me/patient")
    @ResponseStatus(HttpStatus.OK)
    public PatientProfileResponse updatePatientProfile(
            Authentication auth,
            @Valid @RequestBody UpdatePatientProfileRequest request
    ) {
        return profileService.updatePatientProfile(auth.getName(), request);
    }

    @Operation(
            summary = "Обновить профиль врача",
            description = """
                Позволяет врачу обновить свою специальность и контактную информацию.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль врача успешно обновлён",
                    content = @Content(schema = @Schema(implementation = DoctorProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не DOCTOR)", content = @Content)
    })
    @PutMapping("/me/doctor")
    @ResponseStatus(HttpStatus.OK)
    public DoctorProfileResponse updateDoctorProfile(
            Authentication auth,
            @Valid @RequestBody UpdateDoctorProfileRequest request
    ) {
        return profileService.updateDoctorProfile(auth.getName(), request);
    }
}
