package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.game.dto.CreateRoomRequest;
import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;
import br.ufpb.dcx.apps4society.quizapi.game.dto.RoomStateResponse;
import br.ufpb.dcx.apps4society.quizapi.game.model.AdvanceMode;
import br.ufpb.dcx.apps4society.quizapi.game.model.GameStyle;
import br.ufpb.dcx.apps4society.quizapi.game.model.RoomMode;
import br.ufpb.dcx.apps4society.quizapi.game.model.ScoringMode;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Testes unitários do motor multiplayer (em memória). Cobrem o fluxo de sala,
 * a correção da reconexão ("A partida já começou") e a nova mecânica de pular
 * questão com prévia para o líder.
 */
class GameRoomServiceTest {
    // Igual ao COUNTDOWN_SECONDS do serviço (3s) — mais folga para o agendador.
    private static final long COUNTDOWN_WAIT_MS = 3600;

    @Mock
    QuestionRepository questionRepository;
    @Mock
    ThemeRepository themeRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    SimpMessagingTemplate messagingTemplate;

    GameRoomService service;

    private static final Long THEME_ID = 1L;
    private static final String HOST_ID = "host-1";

    private User creator;
    private Theme theme;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        creator = new User(UUID.randomUUID(), "Creator", "c@c.com", "12345678", Role.USER);
        theme = new Theme(THEME_ID, "Tema", creator);
        lenient().when(themeRepository.findById(THEME_ID)).thenReturn(java.util.Optional.of(theme));
        lenient().when(questionRepository.findByThemeId(THEME_ID)).thenReturn(buildQuestions(theme, creator, 3));
        service = new GameRoomService(questionRepository, themeRepository, userRepository, messagingTemplate);
    }

    /** Config com o criador jogando (hostPlays=true) — item "modo do criador". */
    private GameConfig configHostPlays() {
        return new GameConfig(RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.HOST,
                5, 3, null, GameStyle.NORMAL, 12, true);
    }

    /** Config de sala de torneio: criador joga + avanço automático por tempo. */
    private GameConfig configTournament() {
        return new GameConfig(RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.AUTO,
                5, 3, null, GameStyle.NORMAL, 2, true);
    }

    private List<Question> buildQuestions(Theme theme, User creator, int count) {
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Question q = new Question((long) i, "Questão " + i, "img" + i, theme, creator);
            q.addAlternative(new Alternative((long) (i * 10 + 1), "Certa " + i, true, q));
            q.addAlternative(new Alternative((long) (i * 10 + 2), "Errada " + i, false, q));
            questions.add(q);
        }
        return questions;
    }

    private GameConfig config() {
        return new GameConfig(RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.HOST,
                5, 3, null, GameStyle.NORMAL, 12, false);
    }

    private String createRoomWithPlayer(String playerId) {
        RoomStateResponse state = service.createRoom(
                new CreateRoomRequest(HOST_ID, "Prof", THEME_ID, config()));
        String code = state.code();
        service.join(code, playerId, "Aluno", null, null);
        service.setReady(code, playerId, true);
        return code;
    }

    /** Leva a sala até o estado BETWEEN da primeira questão (líder + 1 jogador). */
    private String playFirstQuestionUntilBetween(String playerId) throws InterruptedException {
        String code = createRoomWithPlayer(playerId);
        service.start(code, HOST_ID);
        Thread.sleep(COUNTDOWN_WAIT_MS);
        assertEquals("IN_QUESTION", service.getState(code).status());

        service.answer(code, playerId, firstAlternativeId(0));
        assertEquals("BETWEEN", service.getState(code).status());
        return code;
    }

    private Long firstAlternativeId(int questionIndex) {
        // Alternativa correta da questão de índice informado (id = (i+1)*10 + 1).
        return (long) ((questionIndex + 1) * 10 + 1);
    }

    @Test
    void createRoom_startsInLobbyWithHostReady() {
        RoomStateResponse state = service.createRoom(
                new CreateRoomRequest(HOST_ID, "Prof", THEME_ID, config()));

        assertNotNull(state.code());
        assertEquals("LOBBY", state.status());
        assertEquals(HOST_ID, state.hostId());
        assertEquals(1, state.players().size());
        assertTrue(state.players().get(0).host());
    }

    @Test
    void join_existingPlayerReconnecting_afterStart_doesNotErrorAndStaysInRoom() throws InterruptedException {
        String code = createRoomWithPlayer("p1");
        service.start(code, HOST_ID);
        Thread.sleep(COUNTDOWN_WAIT_MS);
        assertEquals("IN_QUESTION", service.getState(code).status());

        // Reconexão do mesmo jogador no meio da partida: não pode remover nem duplicar.
        assertDoesNotThrow(() -> service.join(code, "p1", "Aluno", null, null));
        long p1Count = service.getState(code).players().stream()
                .filter(p -> p.id().equals("p1")).count();
        assertEquals(1, p1Count);
    }

    @Test
    void start_withPlayerNotReady_stillStartsMatch() throws InterruptedException {
        RoomStateResponse created = service.createRoom(
                new CreateRoomRequest(HOST_ID, "Prof", THEME_ID, config()));
        String code = created.code();
        service.join(code, "p1", "Aluno", null, null); // não deu "pronto"

        service.start(code, HOST_ID);
        Thread.sleep(COUNTDOWN_WAIT_MS);

        assertEquals("IN_QUESTION", service.getState(code).status());
    }

    @Test
    void endQuestion_populatesNextQuestionPreviewForHost() throws InterruptedException {
        // As questões são embaralhadas no start, então os títulos são
        // verificados por presença/estrutura, não por valor fixo.
        String code = playFirstQuestionUntilBetween("p1");

        RoomStateResponse state = service.getState(code);
        assertEquals("BETWEEN", state.status());
        assertNotNull(state.nextQuestion(), "deveria haver prévia da próxima questão");
        assertNotNull(state.nextQuestion().title());
        assertEquals(2, state.nextQuestion().alternatives().size());
        assertEquals(0, state.skippedCount());
    }

    @Test
    void skipNextQuestion_advancesPreviewAndCountsSkip() throws InterruptedException {
        String code = playFirstQuestionUntilBetween("p1");
        String titleBeforeSkip = service.getState(code).nextQuestion().title();

        service.skipNextQuestion(code, HOST_ID);

        RoomStateResponse state = service.getState(code);
        assertEquals("BETWEEN", state.status(), "pular NÃO deve iniciar a questão");
        assertEquals(1, state.skippedCount());
        assertNotNull(state.nextQuestion());
        assertNotEquals(titleBeforeSkip, state.nextQuestion().title(),
                "a prévia deve apontar para outra questão após pular");
    }

    @Test
    void skipNextQuestion_onLastAvailable_hidesPreview() throws InterruptedException {
        String code = playFirstQuestionUntilBetween("p1");

        // Q1 jogada; próxima = Q2. Pular uma vez → próxima = Q3. Pular de novo →
        // não há mais próxima (Q3 seria a última e vira a "atual" a jogar).
        service.skipNextQuestion(code, HOST_ID); // agora aponta Q3
        service.skipNextQuestion(code, HOST_ID); // não há próxima após Q3

        RoomStateResponse state = service.getState(code);
        assertNull(state.nextQuestion(), "sem próxima questão para pular");
        assertEquals(2, state.skippedCount());
    }

    @Test
    void next_afterSkip_startsCorrectQuestion() throws InterruptedException {
        String code = playFirstQuestionUntilBetween("p1");
        service.skipNextQuestion(code, HOST_ID); // pula Q2, prévia agora Q3

        service.next(code, HOST_ID);

        RoomStateResponse state = service.getState(code);
        assertEquals("IN_QUESTION", state.status());
        assertEquals(2, state.currentQuestionIndex(), "deve iniciar a Q3 (índice 2), pulando a Q2");
    }

    @Test
    void skipNextQuestion_byNonHost_isIgnored() throws InterruptedException {
        String code = playFirstQuestionUntilBetween("p1");

        service.skipNextQuestion(code, "p1"); // não é o líder

        assertEquals(0, service.getState(code).skippedCount());
    }

    // ---------- modo "criador participa" (hostPlays) ----------

    @Test
    void hostPlaysFalse_hostAnswerIsIgnoredAndDoesNotScore() throws InterruptedException {
        // Sala padrão (espectador): a resposta do líder é ignorada e o jogador
        // sozinho encerra a questão sem depender do líder.
        String code = createRoomWithPlayer("p1");
        service.start(code, HOST_ID);
        Thread.sleep(COUNTDOWN_WAIT_MS);
        assertEquals("IN_QUESTION", service.getState(code).status());

        service.answer(code, HOST_ID, firstAlternativeId(0)); // líder não conta
        RoomStateResponse afterHost = service.getState(code);
        assertEquals("IN_QUESTION", afterHost.status(), "líder espectador não deve encerrar a questão");
        int hostScore = afterHost.players().stream()
                .filter(p -> p.id().equals(HOST_ID)).findFirst().orElseThrow().score();
        assertEquals(0, hostScore, "líder espectador não pontua");

        service.answer(code, "p1", firstAlternativeId(0));
        assertEquals("BETWEEN", service.getState(code).status());
    }

    @Test
    void hostPlaysTrue_hostCountsForEndAndScores() throws InterruptedException {
        // Uma única questão (determinística) para garantir a alternativa correta.
        lenient().when(questionRepository.findByThemeId(THEME_ID))
                .thenReturn(buildQuestions(theme, creator, 1));

        RoomStateResponse created = service.createRoom(
                new CreateRoomRequest(HOST_ID, "Prof", THEME_ID, configHostPlays()));
        String code = created.code();
        service.join(code, "p1", "Aluno", null, null);
        service.start(code, HOST_ID);
        Thread.sleep(COUNTDOWN_WAIT_MS);
        assertEquals("IN_QUESTION", service.getState(code).status());

        // Só o jogador respondeu: como o líder também joga, a questão NÃO encerra.
        service.answer(code, "p1", firstAlternativeId(0));
        assertEquals("IN_QUESTION", service.getState(code).status(),
                "com hostPlays o líder também precisa responder");

        service.answer(code, HOST_ID, firstAlternativeId(0));
        RoomStateResponse state = service.getState(code);
        assertEquals("BETWEEN", state.status());
        int hostScore = state.players().stream()
                .filter(p -> p.id().equals(HOST_ID)).findFirst().orElseThrow().score();
        assertTrue(hostScore > 0, "líder participante deve pontuar ao acertar");
    }

    // ---------- salas de torneio (auto-início + gerenciadas) ----------

    @Test
    void tournamentRoom_autoStartsWhenBothPlayersConnect() throws InterruptedException {
        RoomStateResponse created = service.createTournamentRoom(
                new CreateRoomRequest(HOST_ID, "P1", THEME_ID, configTournament()), 2);
        String code = created.code();
        assertEquals("LOBBY", service.getState(code).status());

        service.join(code, HOST_ID, "P1", null, null); // 1º adversário conecta
        assertEquals("LOBBY", service.getState(code).status(), "ainda falta o 2º jogador");

        service.join(code, "p2", "P2", null, null); // 2º conecta -> auto-início
        Thread.sleep(COUNTDOWN_WAIT_MS);
        assertEquals("IN_QUESTION", service.getState(code).status());
    }

    @Test
    void tournamentRoom_hostLeaveDoesNotCloseRoom() {
        RoomStateResponse created = service.createTournamentRoom(
                new CreateRoomRequest(HOST_ID, "P1", THEME_ID, configTournament()), 2);
        String code = created.code();
        service.join(code, HOST_ID, "P1", null, null);
        service.join(code, "p2", "P2", null, null);

        service.leave(code, HOST_ID); // numa sala de torneio isso NÃO encerra a sala

        assertDoesNotThrow(() -> service.getState(code),
                "sala de torneio deve sobreviver à saída do líder até o torneio apurar o vencedor");
    }

    @Test
    void normalRoom_hostLeaveClosesRoom() {
        RoomStateResponse created = service.createRoom(
                new CreateRoomRequest(HOST_ID, "Prof", THEME_ID, config()));
        String code = created.code();
        service.join(code, "p1", "Aluno", null, null);

        service.leave(code, HOST_ID); // sala normal: líder saindo encerra

        assertThrows(java.util.NoSuchElementException.class, () -> service.getState(code));
    }
}
