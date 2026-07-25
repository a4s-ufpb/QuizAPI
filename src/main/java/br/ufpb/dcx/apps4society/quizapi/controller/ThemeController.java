package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.service.ThemeService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.ImageSizeLimitExceededException;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/theme")
@Tag(name = "Theme", description = "Themes of Questions")
public class ThemeController {
    private ThemeService service;

    @Autowired
    public ThemeController(ThemeService service) {
        this.service = service;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThemeResponse> insertTheme(@RequestBody @Valid ThemeRequest themeRequest, @RequestHeader("Authorization") String token) throws ThemeAlreadyExistsException, ImageSizeLimitExceededException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.insertTheme(themeRequest, token));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeResponse> insertThemeMultipart(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String materials,
            @RequestParam(required = false) String tagIds,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            @RequestHeader("Authorization") String token) throws ThemeAlreadyExistsException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.insertThemeMultipart(name, description, materials, tagIds, imageFile, imageUrl, token));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> removeTheme(@PathVariable Long id, @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        service.removeTheme(id, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ThemeResponse>> findAllThemes(@RequestParam(value = "page", defaultValue = "0") Integer page,
                                                             @RequestParam(value = "size", defaultValue = "12") Integer size,
                                                             @RequestParam(value = "name", defaultValue = "") String name,
                                                             @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                                                             @RequestHeader(value = "Authorization", required = false) String token){
        Pageable pageable = PageRequest.of(page,size);
        return ResponseEntity.ok(service.findAllThemes(pageable, name, tagIds, token));
    }

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

    @GetMapping(value = "/{id}",produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThemeResponse> findThemeById(@PathVariable Long id,
                                                       @RequestHeader(value = "Authorization", required = false) String token){
        return ResponseEntity.ok(service.findThemeById(id, token));
    }

    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ThemeResponse> updateTheme(@PathVariable Long id,
                                                     @RequestBody @Valid ThemeUpdate themeUpdate,
                                                     @RequestHeader("Authorization") String token) throws UserNotHavePermissionException, ThemeAlreadyExistsException, ImageSizeLimitExceededException {
        return ResponseEntity.ok(service.updateTheme(id,themeUpdate, token));
    }

    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ThemeResponse> updateThemeMultipart(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String materials,
            @RequestParam(required = false) String tagIds,
            @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
            @RequestHeader("Authorization") String token) throws UserNotHavePermissionException, ThemeAlreadyExistsException {
        return ResponseEntity.ok(service.updateThemeMultipart(id, name, description, materials, tagIds, imageFile, imageUrl, token));
    }

    // === Likes ===
    @PostMapping("/{id}/like")
    public ResponseEntity<ThemeResponse> like(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.likeTheme(id, token));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<ThemeResponse> unlike(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.unlikeTheme(id, token));
    }
}
