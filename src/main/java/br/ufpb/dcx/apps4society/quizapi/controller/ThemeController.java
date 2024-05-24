package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.service.ThemeService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ThemeAlreadyExistsException;
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

@RestController
@RequestMapping("/v1/theme")
@Tag(name = "Theme", description = "Themes of Questions")
public class ThemeController {
    private ThemeService service;

    @Autowired
    public ThemeController(ThemeService service) {
        this.service = service;
    }

    @Operation(tags = "Theme", summary = "Insert Theme", responses ={
            @ApiResponse(description = "Success", responseCode = "201", content = @Content(schema = @Schema(implementation = ThemeResponse.class))),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThemeResponse> insertTheme(@RequestBody @Valid ThemeRequest themeRequest, @RequestHeader("Authorization") String token) throws ThemeAlreadyExistsException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertTheme(themeRequest, token));
    }

    @Operation(tags = "Theme", summary = "Remove Theme", responses ={
            @ApiResponse(description = "No Content", responseCode = "204", content = @Content()),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeTheme(@PathVariable Long id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        service.removeTheme(id, token);
        return ResponseEntity.noContent().build();
    }

    @Operation(tags = "Theme", summary = "Find All Themes", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ThemeResponse.class)))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ThemeResponse>> findAllThemes(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                             @RequestParam(value = "size", defaultValue = "12") Integer size,
                                                             @RequestParam(value = "name", defaultValue = "") String name){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findAllThemes(pageable, name));
    }

    @Operation(tags = "Theme", summary = "Find Themes By Creator", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ThemeResponse.class)))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Forbidden", responseCode = "403", content = @Content()),
    })
    @GetMapping(value = "/creator", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ThemeResponse>> findThemesByCreator(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                                   @RequestParam(value = "size", defaultValue = "12") Integer size,
                                                                   @RequestParam(value = "direction", defaultValue = "asc") String direction,
                                                                   @RequestParam(value = "name", defaultValue = "") String name,
                                                                   @RequestHeader("Authorization") String token){
        Sort.Direction directionOfPage = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page,size, Sort.by(directionOfPage, "id"));
        return ResponseEntity.ok(service.findThemesByCreator(token, name, pageable));
    }

    @Operation(tags = "Theme", summary = "Find Theme", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = ThemeResponse.class))),
            @ApiResponse(description = "Not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
    })
    @GetMapping(value = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThemeResponse> findThemeById(@PathVariable Long id){
        return ResponseEntity.ok(service.findThemeById(id));
    }

    @Operation(tags = "Theme", summary = "Update Theme", responses ={
            @ApiResponse(description = "Success", responseCode = "200", content = @Content(schema = @Schema(implementation = ThemeResponse.class))),
            @ApiResponse(description = "not Found", responseCode = "404", content = @Content()),
            @ApiResponse(description = "Bad Request", responseCode = "400", content = @Content()),
            @ApiResponse(description = "Internal Server Error", responseCode = "500", content = @Content()),
            @ApiResponse(description = "Unauthorized", responseCode = "403", content = @Content())
    } )
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThemeResponse> updateTheme(@PathVariable Long id,
                                                     @RequestBody @Valid ThemeUpdate themeUpdate,
                                                     @RequestHeader("Authorization") String token) throws UserNotHavePermissionException, ThemeAlreadyExistsException {
        return ResponseEntity.ok(service.updateTheme(id,themeUpdate, token));
    }

}
