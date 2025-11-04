package org.aleksanyan.medserver.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.aleksanyan.medserver.dto.request.TakeMedicineRequest;
import org.aleksanyan.medserver.dto.response.ScheduleItemResponse;
import org.aleksanyan.medserver.service.ScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/patient/schedule")
@RequiredArgsConstructor
@Tag(name = "Patient Schedule", description = "Расписание приёма лекарств для пациента")
@SecurityRequirement(name = "bearerAuth")
public class PatientScheduleController {

    private final ScheduleService scheduleService;

    @Operation(
            summary = "Получить своё расписание лечения",
            description = """
                Возвращает список элементов расписания (ScheduleItem) для текущего пациента.
                Можно указать диапазон дат (`from`, `to`). Если не указаны, берётся ±7 дней от текущего времени.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список расписания успешно получен",
                    content = @Content(schema = @Schema(implementation = ScheduleItemResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT или токен недействителен)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не PATIENT)", content = @Content)
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ScheduleItemResponse> getSchedule(
            Authentication auth,
            @Parameter(description = "Дата начала диапазона (ISO 8601)", example = "2025-11-01T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @Parameter(description = "Дата конца диапазона (ISO 8601)", example = "2025-11-10T23:59:59Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to
    ) {
        return scheduleService.getPatientSchedule(auth.getName(), from, to);
    }

    @Operation(
            summary = "Отметить приём лекарства",
            description = """
                Отмечает конкретный элемент расписания как «принято».
                Можно добавить необязательную заметку (`note`), например «после еды».
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Лекарство отмечено как принято",
                    content = @Content(schema = @Schema(implementation = ScheduleItemResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не PATIENT или чужой элемент)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Элемент расписания не найден", content = @Content)
    })
    @PatchMapping("/{itemId}/taken")
    @ResponseStatus(HttpStatus.OK)
    public ScheduleItemResponse markAsTaken(
            Authentication auth,
            @Parameter(description = "ID элемента расписания", example = "123")
            @PathVariable Long itemId,
            @Valid @RequestBody(required = false)
            TakeMedicineRequest request
    ) {
        return scheduleService.markItemAsTaken(auth.getName(), itemId, request);
    }
}
