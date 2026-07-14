package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.matchhistory.MatchHistoryResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.MatchHistory;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.MatchHistoryRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchHistoryServiceTest {
    @Mock MatchHistoryRepository matchHistoryRepository;
    @Mock UserRepository userRepository;
    @Mock UserService userService;
    @Mock WalletService walletService;
    @InjectMocks MatchHistoryService matchHistoryService;

    User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User(UUID.randomUUID(), "User", "u@u.com", "12345678", Role.USER);
        when(userService.findUserByToken("token")).thenReturn(user);
    }

    @Test
    void recordMatch_grantsXpAndCoins() {
        MatchHistoryRequest request = new MatchHistoryRequest("SINGLE_PLAYER", "Geografia", 4, 10);

        MatchHistoryResponse response = matchHistoryService.recordMatch(request, "token");

        assertNotNull(response);
        verify(matchHistoryRepository).save(any(MatchHistory.class));
        // 4 acertos * 10 XP = 40 (sem bônus, pois não gabaritou).
        assertEquals(40, user.getXp());
        // 4 acertos * 30 moedas = 120 creditadas via carteira.
        verify(walletService).earn(eq(user), eq(120), anyString());
    }

    @Test
    void recordMatch_perfectScore_addsBonuses() {
        MatchHistoryRequest request = new MatchHistoryRequest("SINGLE_PLAYER", "Geografia", 10, 10);

        matchHistoryService.recordMatch(request, "token");

        // 10*10 + 20 de bônus por gabaritar.
        assertEquals(120, user.getXp());
        // 10*30 + 50 de bônus.
        verify(walletService).earn(eq(user), eq(350), anyString());
    }
}
