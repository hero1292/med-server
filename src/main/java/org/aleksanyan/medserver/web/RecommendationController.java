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
import org.aleksanyan.medserver.dto.request.CreateRecommendationRequest;
import org.aleksanyan.medserver.dto.request.UpdateRecommendationRequest;
import org.aleksanyan.medserver.dto.response.RecommendationResponse;
import org.aleksanyan.medserver.dto.response.RecommendationSummaryResponse;
import org.aleksanyan.medserver.service.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/recommendations")
@RequiredArgsConstructor
@Tag(name = "Doctor Recommendations", description = "Управление рекомендациями врачом: создание, изменение, удаление, просмотр")
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "Создать новую рекомендацию пациенту",
            description = """
                    Врач создаёт новую рекомендацию для пациента. 
                    На основе параметров автоматически генерируется график приёма (schedule).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Рекомендация успешно создана",
                    content = @Content(schema = @Schema(implementation = RecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не DOCTOR)", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse createRecommendation(
            Authentication auth,
            @Valid @RequestBody CreateRecommendationRequest request
    ) {
        return recommendationService.createRecommendation(auth.getName(), request);
    }

    @Operation(
            summary = "Получить список рекомендаций",
            description = """
                    Возвращает список рекомендаций врача.
                    Можно указать `patientId`, чтобы получить рекомендации конкретного пациента.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список рекомендаций успешно получен",
                    content = @Content(schema = @Schema(implementation = RecommendationSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не DOCTOR)", content = @Content)
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<RecommendationSummaryResponse> getRecommendations(
            Authentication auth,
            @Parameter(description = "ID пациента (опционально)", example = "42")
            @RequestParam(required = false) Long patientId
    ) {
        return recommendationService.getRecommendationsByDoctor(auth.getName(), patientId);
    }

    @Operation(
            summary = "Обновить существующую рекомендацию",
            description = """
                    Обновляет данные рекомендации и автоматически перегенерирует будущие элементы расписания.
                    Старые (непринятые) записи графика заменяются на новые.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Рекомендация успешно обновлена",
                    content = @Content(schema = @Schema(implementation = RecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации данных", content = @Content),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не DOCTOR или не владелец рекомендации)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Рекомендация не найдена", content = @Content)
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public RecommendationResponse updateRecommendation(
            Authentication auth,
            @Parameter(description = "ID рекомендации", example = "101")
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecommendationRequest request
    ) {
        return recommendationService.updateRecommendation(auth.getName(), id, request);
    }

    @Operation(
            summary = "Удалить рекомендацию",
            description = """
                    Удаляет рекомендацию врача и все связанные с ней элементы расписания (schedule items).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Рекомендация успешно удалена"),
            @ApiResponse(responseCode = "401", description = "Неавторизован (нет JWT)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (роль не DOCTOR или не владелец рекомендации)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Рекомендация не найдена", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecommendation(
            Authentication auth,
            @Parameter(description = "ID рекомендации", example = "101")
            @PathVariable Long id
    ) {
        recommendationService.deleteRecommendation(auth.getName(), id);
    }
}
