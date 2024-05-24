package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import br.ufpb.dcx.apps4society.quizapi.service.QuestionService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/question")
@Tag(name = "Question", description = "Questions of Quiz")
public class QuestionController {
    private QuestionService service;

    @Autowired
    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @Operation(tags = "Question", summary = "Insert Question", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Theme Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/{idTheme}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionResponse> insertQuestion(@RequestBody @Valid QuestionRequest questionRequest, @PathVariable Long idTheme,
                                                           @RequestHeader(value = "Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertQuestion(questionRequest, idTheme, token));
    }

    @Operation(tags = "Question", summary = "Remove Question", responses ={
            @ApiResponse(description = "No content", responseCode = "204", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeQuestion(@PathVariable Long id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        service.removeQuestion(id, token);
        return ResponseEntity.noContent().build();
    }

    @Operation(tags = "Question", summary = "Find Question", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionResponse> findQuestionById(@PathVariable Long id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.ok(service.findQuestionById(id,token));
    }

    @Operation(tags = "Question", summary = "Update Question", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PatchMapping(value = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QuestionResponse> updateQuestion(@PathVariable Long id, @RequestBody @Valid QuestionUpdate questionUpdate,
                                                           @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.ok(service.updateQuestion(id, questionUpdate, token));
    }

    @Operation(tags = "Question", summary = "Find 10 Questions by Theme", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuestionResponse.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
    } )
    @GetMapping(value = "/quiz/{idTheme}")
    public ResponseEntity<List<QuestionResponse>> find10QuestionsByThemeId(@PathVariable Long idTheme){
        return ResponseEntity.ok(service.find10QuestionsByThemeId(idTheme));
    }

    @Operation(tags = "Question", summary = "Find Questions by Creator and Theme", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuestionResponse.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/creator/theme/{idTheme}")
    public ResponseEntity<Page<QuestionResponse>> findQuestionsByCreator(@RequestHeader("Authorization") String token,
                                                                         @PathVariable Long idTheme,
                                                                         @RequestParam(value = "title", defaultValue = "") String title,
                                                                         @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                         @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                                         @RequestParam(value = "direction", defaultValue = "asc") String direction){
        Sort.Direction pageDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page,size, Sort.by(pageDirection,"id"));
        return ResponseEntity.ok(service.findQuestionsByCreatorAndTheme(token, title, idTheme, pageable));
    }

    @Operation(tags = "Question", summary = "Find 10 Questions by Theme and Creator", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = QuestionResponse.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/creator/quiz/{idTheme}")
    public ResponseEntity<List<QuestionResponse>> find10QuestionsByThemeIdAndCreatorId(@PathVariable Long idTheme,
                                                                                       @RequestHeader("Authorization") String token){
        return ResponseEntity.ok(service.find10QuestionsByThemeIdAndCreatorId(idTheme, token));
    }
}
