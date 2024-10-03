package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StatisticResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.StudentName;
import br.ufpb.dcx.apps4society.quizapi.dto.statistic.ThemeName;
import br.ufpb.dcx.apps4society.quizapi.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/statistic")
@Tag(name = "Statistic", description = "Statistics per conclusion")
public class StatisticController {
    private final StatisticService statisticService;

    public StatisticController(StatisticService statisticService) {
        this.statisticService = statisticService;
    }

    @Operation(tags = "Statistic", summary = "Insert Statistic", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = StatisticResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
    } )
    @PostMapping
    public ResponseEntity<StatisticResponse> insertStatistic(@RequestBody @Valid StatisticRequest statisticRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statisticService.insertStatistic(statisticRequest));
    }

    @Operation(tags = "Statistic", summary = "Find Statistics By Creator", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StatisticResponse.class)))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content()),
    })
    @GetMapping(value = "/{creatorId}")
    public ResponseEntity<Page<StatisticResponse>> findAllStatisticByCreator(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                             @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                                             @RequestParam(value = "studentName", defaultValue = "") String studentName,
                                                                             @RequestParam(value = "themeName", defaultValue = "") String themeName,
                                                                             @RequestParam(value = "startDate", defaultValue = "") LocalDate startDate,
                                                                             @RequestParam(value = "endDate", defaultValue = "") LocalDate endDate,
                                                                             @PathVariable UUID creatorId) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(statisticService.findAllStatisticsByCreator(pageable, creatorId, studentName, themeName, startDate, endDate));
    }

    @Operation(tags = "Statistic", summary = "Find Distinct Theme Names By Creator", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StatisticResponse.class)))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content()),
    })
    @GetMapping(value = "/theme/{creatorId}")
    public ResponseEntity<List<ThemeName>> findDistinctThemeNameByCreatorId(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(statisticService.findDistinctThemeNameByCreatorId(creatorId));
    }

    @Operation(tags = "Statistic", summary = "Find Distinct Student Names By Creator", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = StatisticResponse.class)))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content()),
    })
    @GetMapping(value = "/student/{creatorId}")
    public ResponseEntity<List<StudentName>> findDistinctStudentNameByCreatorId(@PathVariable UUID creatorId) {
        return ResponseEntity.ok(statisticService.findDistinctStudentNameByCreatorId(creatorId));
    }
}
