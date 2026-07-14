package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.achievement.AchievementResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.FriendshipStatus;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.FriendshipRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.MatchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AchievementServiceTest {
    @Mock MatchHistoryRepository matchHistoryRepository;
    @Mock FriendshipRepository friendshipRepository;
    @InjectMocks AchievementService achievementService;

    User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User(UUID.randomUUID(), "User", "u@u.com", "12345678", Role.USER);
    }

    private boolean unlocked(List<AchievementResponse> list, String code) {
        return list.stream().filter(a -> a.code().equals(code)).findFirst()
                .map(AchievementResponse::unlocked).orElse(false);
    }

    @Test
    void findAchievements_noActivity_allLocked() {
        when(matchHistoryRepository.countByUser(user)).thenReturn(0L);
        when(matchHistoryRepository.countPerfectMatches(user)).thenReturn(0L);
        when(friendshipRepository.findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of());

        List<AchievementResponse> achievements = achievementService.findAchievements(user);

        assertFalse(achievements.isEmpty());
        assertTrue(achievements.stream().noneMatch(AchievementResponse::unlocked));
    }

    @Test
    void findAchievements_firstMatchUnlocksButNotTen() {
        when(matchHistoryRepository.countByUser(user)).thenReturn(1L);
        when(matchHistoryRepository.countPerfectMatches(user)).thenReturn(0L);
        when(friendshipRepository.findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of());

        List<AchievementResponse> achievements = achievementService.findAchievements(user);

        assertTrue(unlocked(achievements, "FIRST_MATCH"));
        assertFalse(unlocked(achievements, "TEN_MATCHES"));
    }

    @Test
    void findAchievements_perfectMatchUnlocksPerfectScore() {
        when(matchHistoryRepository.countByUser(user)).thenReturn(3L);
        when(matchHistoryRepository.countPerfectMatches(user)).thenReturn(1L);
        when(friendshipRepository.findAllByUserAndStatus(user, FriendshipStatus.ACCEPTED))
                .thenReturn(List.of());

        List<AchievementResponse> achievements = achievementService.findAchievements(user);

        assertTrue(unlocked(achievements, "PERFECT_SCORE"));
        assertFalse(unlocked(achievements, "FIVE_PERFECT"));
    }
}
