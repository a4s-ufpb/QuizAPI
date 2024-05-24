package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse;
import br.ufpb.dcx.apps4society.quizapi.service.AlternativeService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.AlternativeCorrectDuplicateException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.FalseAlternativesOnlyException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.LimitOfAlternativesException;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/v1/alternative")
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

    @Operation(tags = "Alternative", summary = "Insert All Alternative", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlternativeResponse.class)))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Question Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/all/{idQuestion}")
    public ResponseEntity<List<AlternativeResponse>> insertAllAlternatives(@RequestBody @Valid AlternativeRequest [] alternativeRequests,
                                                                           @PathVariable Long idQuestion) throws FalseAlternativesOnlyException, AlternativeCorrectDuplicateException, LimitOfAlternativesException {
        List<AlternativeResponse> responses = new ArrayList<>();
        for (AlternativeRequest a: alternativeRequests){
            AlternativeResponse response = insertAlternative(a, idQuestion).getBody();
            responses.add(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @Operation(tags = "Alternative", summary = "Update Alternative", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = AlternativeResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlternativeResponse> updateAlternative(@PathVariable Long id, @RequestBody @Valid AlternativeUpdate alternativeUpdate,
                                                                 @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.ok(service.updateAlternative(id, alternativeUpdate, token));
    }

    @Operation(tags = "Alternative", summary = "Remove Alternative", responses ={
            @ApiResponse(description = "Success", responseCode = "204", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeAlternative(@PathVariable Long id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        service.removeAlternative(id,token);
        return ResponseEntity.noContent().build();
    }

}
