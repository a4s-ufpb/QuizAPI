package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseStatisticDTO;
import br.ufpb.dcx.apps4society.quizapi.service.ResponseService;
import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/response")
@Tag(name = "Response", description = "Responses of Users")
public class ResponseController {
    private ResponseService service;

    @Autowired
    public ResponseController(ResponseService service) {
        this.service = service;
    }

    @Operation(tags = "Response", summary = "Insert Response", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = ResponseDTO.class))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(value = "/{idUser}/{idQuestion}/{idAlternative}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseDTO> insertResponse(@PathVariable UUID idUser,
                                                      @PathVariable Long idQuestion,
                                                      @PathVariable Long idAlternative){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertResponse(idUser, idQuestion, idAlternative));
    }

    @Operation(tags = "Response", summary = "Find All Responses", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ResponseDTO>> findAllResponses(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                              @RequestParam(value = "size", defaultValue = "20") Integer size){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findAllResponses(pageable));
    }

    @Operation(tags = "Response", summary = "Find Responses by User", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ResponseDTO>> findResponsesByUser(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                              @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                              @RequestHeader("Authorization") String token){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findResponsesByUser(pageable, token));
    }

    @Operation(tags = "Response", summary = "Find Responses by Question Creator", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/question/creator")
    public ResponseEntity<Page<ResponseDTO>> findResponsesByQuestionCreator(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                            @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                                            @RequestHeader("Authorization") String token){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findResponsesByQuestionCreator(pageable,token));
    }

    @Operation(tags = "Response", summary = "Find Responses by Question Id", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/question/creator/id")
    public ResponseEntity<Page<ResponseDTO>> findResponsesByQuestionId(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                            @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                                            @RequestParam(value = "questionId", defaultValue = "") Long questionId,
                                                                            @RequestHeader("Authorization") String token){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findResponsesByQuestionId(pageable,token, questionId));
    }

    @Operation(tags = "Response", summary = "Find Responses by User Name", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/question/creator/name")
    public ResponseEntity<Page<ResponseDTO>> findResponsesByUserName(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                            @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                                            @RequestParam(value = "name", defaultValue = "") String name,
                                                                            @RequestHeader("Authorization") String token){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findResponsesByUserName(pageable,token, name));
    }

    @Operation(tags = "Response", summary = "Find Responses by Date", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/question/creator/date")
    public ResponseEntity<Page<ResponseDTO>> findResponsesByDate(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                            @RequestParam(value = "size", defaultValue = "20") Integer size,
                                                                            @RequestParam(value = "currentDate", defaultValue = "") LocalDate currentDate,
                                                                            @RequestParam(value = "finalDate", defaultValue = "") LocalDate finalDate,
                                                                            @RequestHeader("Authorization") String token){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findResponsesByDate(pageable,token, currentDate, finalDate));
    }

    @Operation(tags = "Response", summary = "Find Responses Statistics", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseStatisticDTO.class)))),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @GetMapping(value = "/statistic/{themeName}")
    public ResponseEntity<List<ResponseStatisticDTO>> findResponsesStatistics(@RequestHeader("Authorization") String token,
                                                                              @PathVariable String themeName){
        return ResponseEntity.ok(service.findStatisticResponse(token,themeName));
    }

    @Operation(tags = "Response", summary = "Remove Response", responses ={
            @ApiResponse(description = "Success", responseCode = "204", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeResponse(@PathVariable Long id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        service.removeResponse(id, token);
        return ResponseEntity.noContent().build();
    }
}
