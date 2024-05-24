package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.score.ScoreResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/score")
@Tag(name = "Score", description = "Score of Quiz")
public class ScoreController {
    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Operation(tags = "Score", summary = "Insert Score", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = ScoreResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/{userId}/{themeId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScoreResponse> insertScore(@RequestBody @Valid ScoreRequest scoreRequest,
                                                     @PathVariable UUID userId,
                                                     @PathVariable Long themeId){
        return ResponseEntity.status(HttpStatus.CREATED).body(scoreService.insertScore(scoreRequest, userId, themeId));
    }

    @Operation(tags = "Score", summary = "Find Scores by Theme", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScoreResponse.class)))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/{themeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ScoreResponse>> findRankingByTheme(@PathVariable Long themeId){
        return ResponseEntity.ok(scoreService.findRankingByTheme(themeId));
    }
}
