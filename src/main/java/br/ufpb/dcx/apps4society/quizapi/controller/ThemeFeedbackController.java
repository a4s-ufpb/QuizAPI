package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.feedback.ThemeFeedbackResponse;
import br.ufpb.dcx.apps4society.quizapi.service.ThemeFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/theme-feedback")
public class ThemeFeedbackController {
    private final ThemeFeedbackService service;

    public ThemeFeedbackController(ThemeFeedbackService service) {
        this.service = service;
    }

    @PostMapping("/theme/{themeId}")
    public ResponseEntity<ThemeFeedbackResponse> create(@PathVariable Long themeId,
                                                        @RequestBody @Valid ThemeFeedbackRequest req,
                                                        @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(themeId, req, token));
    }

    @GetMapping("/theme/{themeId}")
    public ResponseEntity<List<ThemeFeedbackResponse>> listByTheme(@PathVariable Long themeId) {
        return ResponseEntity.ok(service.listByTheme(themeId));
    }

    @GetMapping("/theme/{themeId}/count")
    public ResponseEntity<Long> count(@PathVariable Long themeId) {
        return ResponseEntity.ok(service.count(themeId));
    }
}
