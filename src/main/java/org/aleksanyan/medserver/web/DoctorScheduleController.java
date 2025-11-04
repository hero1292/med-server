package org.aleksanyan.medserver.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.service.DoctorScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/doctor/patients")
@RequiredArgsConstructor
@Tag(name = "Doctor Schedule", description = "Просмотр расписания пациентов (для врача)")
@SecurityRequirement(name = "bearerAuth")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @Operation(
            summary = "Получить расписание выбранного пациента",
            description = """
                    Возвращает список запланированных приёмов пациента (schedule items) в заданном диапазоне дат.
                    Только врач, к которому прикреплён пациент, имеет доступ к данным.
                    Параметры `from` и `to` — необязательны, по умолчанию ±7 дней от текущей даты.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Расписание успешно получено",
                    content = @Content(schema = @Schema(implementation = ScheduleItemResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неавторизован (JWT отсутствует или истёк)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещён (пациент не принадлежит врачу или роль не DOCTOR)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пациент не найден",
                    content = @Content
            )
    })
    @GetMapping("/{patientId}/schedule")
    @ResponseStatus(HttpStatus.OK)
    public List<ScheduleItemResponse> getPatientSchedule(
            Authentication auth,
            @Parameter(description = "ID пациента", example = "42")
            @PathVariable Long patientId,
            @Parameter(description = "Дата начала диапазона (ISO 8601)", example = "2025-11-01T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @Parameter(description = "Дата конца диапазона (ISO 8601)", example = "2025-11-10T23:59:59Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return doctorScheduleService.getPatientSchedule(auth.getName(), patientId, from, to);
    }
}
