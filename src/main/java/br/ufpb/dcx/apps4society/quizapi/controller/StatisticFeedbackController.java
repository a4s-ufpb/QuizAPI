package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.feedback.StatisticFeedbackResponse;
import br.ufpb.dcx.apps4society.quizapi.service.StatisticFeedbackService;
import br.ufpb.dcx.apps4society.quizapi.service.exception.UserNotHavePermissionException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/statistic-feedback")
public class StatisticFeedbackController {
    private final StatisticFeedbackService service;

    public StatisticFeedbackController(StatisticFeedbackService service) {
        this.service = service;
    }

    @PostMapping("/statistic/{statisticId}")
    public ResponseEntity<StatisticFeedbackResponse> create(@PathVariable Long statisticId,
                                                            @RequestBody @Valid StatisticFeedbackRequest req,
                                                            @RequestHeader("Authorization") String token) throws UserNotHavePermissionException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(statisticId, req, token));
    }

    @GetMapping("/statistic/{statisticId}")
    public ResponseEntity<List<StatisticFeedbackResponse>> byStatistic(@PathVariable Long statisticId) {
        return ResponseEntity.ok(service.listByStatistic(statisticId));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<StatisticFeedbackResponse>> mine(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.listMine(token));
    }

    @GetMapping("/creator/mine")
    public ResponseEntity<List<StatisticFeedbackResponse>> creatorMine(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(service.listByCreator(token));
    }
}
