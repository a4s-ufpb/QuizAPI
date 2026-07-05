package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.achievement.AchievementResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryResponse;
import br.ufpb.dcx.apps4society.quizapi.service.AchievementService;
import br.ufpb.dcx.apps4society.quizapi.service.MatchHistoryService;
import br.ufpb.dcx.apps4society.quizapi.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/match-history")
@Tag(name = "MatchHistory", description = "Persisted match history, XP and achievements")
public class MatchHistoryController {
    private final MatchHistoryService matchHistoryService;
    private final AchievementService achievementService;
    private final UserService userService;

    public MatchHistoryController(MatchHistoryService matchHistoryService, AchievementService achievementService, UserService userService) {
        this.matchHistoryService = matchHistoryService;
        this.achievementService = achievementService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<MatchHistoryResponse> recordMatch(@RequestBody @Valid MatchHistoryRequest request,
                                                              @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchHistoryService.recordMatch(request, token));
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<MatchHistoryResponse>> findMyHistory(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size,
                                                                      @RequestHeader("Authorization") String token) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(matchHistoryService.findMyHistory(token, pageable));
    }

    @GetMapping("/achievements")
    public ResponseEntity<List<AchievementResponse>> findMyAchievements(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(achievementService.findAchievements(userService.findUserByToken(token)));
    }
}
