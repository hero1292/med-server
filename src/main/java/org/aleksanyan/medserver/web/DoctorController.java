package org.aleksanyan.medserver.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.dto.response.PatientShortResponse;
import org.aleksanyan.medserver.service.DoctorService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Tag(name = "Doctor", description = "Функционал врача — список пациентов, рекомендации и расписания")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    @Operation(
            summary = "Получить список своих пациентов",
            description = """
                Возвращает краткую информацию о пациентах, прикреплённых к врачу.
                Требуется авторизация по JWT токену (роль: DOCTOR).
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пациентов успешно получен",
                    content = @Content(schema = @Schema(implementation = PatientShortResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT или истёкший токен)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не DOCTOR)", content = @Content)
    })
    @GetMapping("/patients")
    @ResponseStatus(HttpStatus.OK)
    public List<PatientShortResponse> getMyPatients(Authentication auth) {
        return doctorService.getMyPatients(auth.getName());
    }
}
