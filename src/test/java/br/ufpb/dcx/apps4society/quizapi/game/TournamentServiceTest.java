package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateTournamentRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.TournamentStateResponse;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Testes do modo torneio: lobby, travamento das chaves (4/8/16), escolha do quiz
 * por rodada, início do chaveamento e remoção/saída de jogadores. Usa um
 * {@link GameRoomService} real (em memória) com repositórios mockados.
 */
class TournamentServiceTest {
    private static final Long THEME_ID = 1L;
    private static final String HOST_ID = "host-1";

    @Mock QuestionRepository questionRepository;
    @Mock ThemeRepository themeRepository;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    TournamentService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        User creator = new User(UUID.randomUUID(), "Creator", "c@c.com", "12345678", Role.USER);
        Theme theme = new Theme(THEME_ID, "Tema", creator);
        lenient().when(themeRepository.findById(THEME_ID)).thenReturn(Optional.of(theme));
        GameRoomService gameRoomService =
                new GameRoomService(questionRepository, themeRepository, userRepository, messagingTemplate);
        service = new TournamentService(gameRoomService, themeRepository, userRepository, messagingTemplate);
    }

    private String createTournament() {
        TournamentStateResponse state = service.createTournament(new CreateTournamentRequest(
                HOST_ID, "Host", "Torneio", null, 5, 30, 8, ""));
        return state.code();
    }

    private void joinPlayers(String code, int howMany) {
        for (int i = 1; i <= howMany; i++) {
            service.join(code, "p" + i, "Jogador " + i, "");
        }
    }

    @Test
    void createTournament_startsInLobbyWithHostAsPlayer() {
        TournamentStateResponse state = service.createTournament(new CreateTournamentRequest(
                HOST_ID, "Host", "Torneio", null, 5, 30, 8, ""));

        assertNotNull(state.code());
        assertEquals("LOBBY", state.status());
        assertEquals(HOST_ID, state.hostId());
        assertEquals(1, state.players().size());
        assertEquals(8, state.maxPlayers());
        assertTrue(state.players().get(0).host());
    }

    @Test
    void configure_requiresExactBracketSize() {
        String code = createTournament();
        joinPlayers(code, 2); // total = 3 (host + 2) -> inválido

        assertThrows(IllegalStateException.class, () -> service.configure(code, HOST_ID));
    }

    @Test
    void configure_withFourPlayers_locksAndSizesRounds() {
        String code = createTournament();
        joinPlayers(code, 3); // total = 4

        TournamentStateResponse state = service.configure(code, HOST_ID);

        assertEquals("CONFIGURING", state.status());
        assertEquals(4, state.bracketSize());
        assertEquals(2, state.roundThemes().size(), "4 jogadores => 2 rodadas");
        assertTrue(state.roundThemes().stream().allMatch(r -> r.themeId() == null));
    }

    @Test
    void configure_onlyHostCanLock() {
        String code = createTournament();
        joinPlayers(code, 3);

        assertThrows(IllegalStateException.class, () -> service.configure(code, "p1"));
    }

    @Test
    void join_afterConfiguring_isRejected() {
        String code = createTournament();
        joinPlayers(code, 3);
        service.configure(code, HOST_ID);

        assertThrows(IllegalStateException.class, () -> service.join(code, "late", "Atrasado", ""));
    }

    @Test
    void start_requiresAllRoundThemesChosen() {
        String code = createTournament();
        joinPlayers(code, 3);
        service.configure(code, HOST_ID);
        service.setRoundTheme(code, HOST_ID, 0, THEME_ID); // falta a rodada 1

        assertThrows(IllegalStateException.class, () -> service.start(code, HOST_ID));
    }

    @Test
    void start_withAllThemes_launchesFirstRoundMatches() {
        String code = createTournament();
        joinPlayers(code, 3);
        service.configure(code, HOST_ID);
        service.setRoundTheme(code, HOST_ID, 0, THEME_ID);
        service.setRoundTheme(code, HOST_ID, 1, THEME_ID);

        TournamentStateResponse state = service.start(code, HOST_ID);

        assertEquals("IN_PROGRESS", state.status());
        assertEquals(1, state.rounds().size());
        assertEquals(2, state.rounds().get(0).size(), "4 jogadores => 2 confrontos na 1ª rodada");
        assertTrue(state.rounds().get(0).stream().allMatch(m -> m.roomCode() != null),
                "cada confronto deve ter uma sala criada");
    }

    @Test
    void kick_removesPlayer_andOnlyHostCan() {
        String code = createTournament();
        joinPlayers(code, 1); // host + p1

        assertThrows(IllegalStateException.class, () -> service.kick(code, "p1", HOST_ID));

        TournamentStateResponse state = service.kick(code, HOST_ID, "p1");
        assertEquals(1, state.players().size());
        assertTrue(state.players().stream().noneMatch(p -> p.id().equals("p1")));
    }

    @Test
    void leave_byHost_endsTournament() {
        String code = createTournament();
        joinPlayers(code, 1);

        service.leave(code, HOST_ID);

        assertThrows(NoSuchElementException.class, () -> service.getState(code));
    }

    @Test
    void reopen_returnsToLobbyAndClearsThemes() {
        String code = createTournament();
        joinPlayers(code, 3);
        service.configure(code, HOST_ID);
        service.setRoundTheme(code, HOST_ID, 0, THEME_ID);

        TournamentStateResponse state = service.reopen(code, HOST_ID);

        assertEquals("LOBBY", state.status());
        assertEquals(0, state.bracketSize());
        assertTrue(state.roundThemes().isEmpty());
    }
}
