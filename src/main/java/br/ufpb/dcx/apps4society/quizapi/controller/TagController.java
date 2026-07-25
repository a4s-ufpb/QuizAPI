package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.tag.TagResponse;
import br.ufpb.dcx.apps4society.quizapi.service.TagService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tag")
@Tag(name = "Tag", description = "Tags per user")
public class TagController {
    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@RequestBody @Valid TagRequest req,
                                              @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req, token));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TagResponse>> mine(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.listMine(token));
    }

    @GetMapping("/creator/{uuid}")
    public ResponseEntity<List<TagResponse>> byCreator(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.listByCreator(uuid));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TagResponse> update(@PathVariable Long id, @RequestBody @Valid TagRequest req,
                                              @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.ok(service.update(id, req, token));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        service.delete(id, token);
        return ResponseEntity.noContent().build();
    }
}
