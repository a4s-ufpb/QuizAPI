package com.ronyelison.quiz.controller;

import com.ronyelison.quiz.dto.alternative.AlternativeRequest;
import com.ronyelison.quiz.dto.alternative.AlternativeResponse;
import com.ronyelison.quiz.dto.alternative.AlternativeUpdate;
import com.ronyelison.quiz.service.AlternativeService;
import com.ronyelison.quiz.service.exception.AlternativeCorrectDuplicateException;
import com.ronyelison.quiz.service.exception.FalseAlternativesOnlyException;
import com.ronyelison.quiz.service.exception.LimitOfAlternativesException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alternative")
@Tag(name = "Alternative", description = "Alternatives of Questions")
public class AlternativeController {
    private AlternativeService service;

    @Autowired
    public AlternativeController(AlternativeService service) {
        this.service = service;
    }

    @Operation(tags = "Alternative", summary = "Insert Alternative", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = AlternativeResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Question Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/{idQuestion}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlternativeResponse> insertAlternative(@RequestBody @Valid AlternativeRequest alternative, @PathVariable Long idQuestion)
            throws FalseAlternativesOnlyException, AlternativeCorrectDuplicateException, LimitOfAlternativesException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertAlternative(alternative, idQuestion));
    }

    @Operation(tags = "Alternative", summary = "Update Alternative", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = AlternativeResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlternativeResponse> updateAlternative(@PathVariable Long id, @RequestBody @Valid AlternativeUpdate alternativeUpdate){
        return ResponseEntity.ok(service.updateAlternative(id, alternativeUpdate));
    }
}
