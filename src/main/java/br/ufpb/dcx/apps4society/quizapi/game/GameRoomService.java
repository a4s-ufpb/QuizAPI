package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.game.dto.*;
import br.ufpb.dcx.apps4society.quizapi.game.model.*;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Motor do modo multiplayer (estilo Kahoot), totalmente em memória.
 * Nada de gameplay é persistido: salas, jogadores, respostas e pontuação vivem
 * apenas enquanto a sala existe. Jogadores são convidados (sem login).
 */
@Service
public class GameRoomService {
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int RESULT_DELAY_SECONDS = 5;
    private static final int SCORE_MAX = 1000;
    private static final int SCORE_MIN_CORRECT = 500;

    private final Map<String, GameRoom> rooms = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final QuestionRepository questionRepository;
    private final ThemeRepository themeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GameRoomService(QuestionRepository questionRepository,
                           ThemeRepository themeRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.questionRepository = questionRepository;
        this.themeRepository = themeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // ---------- criação / consulta ----------

    public synchronized RoomStateResponse createRoom(CreateRoomRequest request) {
        GameConfig config = (request.config() != null ? request.config() : GameConfig.defaults()).withDefaults();
        String code = generateUniqueCode();
        String hostId = request.hostId() != null ? request.hostId() : UUID.randomUUID().toString();

        GameRoom room = new GameRoom(code, hostId, config);
        GamePlayer host = new GamePlayer(hostId, safeName(request.hostName(), "Host"), true);
        host.setReady(true);
        room.getPlayers().put(hostId, host);

        applyRoomMode(room, config.roomMode());
        if (request.themeId() != null) {
            setTheme(room, request.themeId());
        }
        rooms.put(code, room);
        return toState(room);
    }

    public synchronized RoomStateResponse getState(String code) {
        return toState(requireRoom(code));
    }

    // ---------- lobby ----------

    public synchronized void join(String code, String playerId, String name) {
        GameRoom room = requireRoom(code);
        if (room.getStatus() != RoomStatus.LOBBY) {
            sendError(code, "A partida já começou.");
            return;
        }
        room.getPlayers().computeIfAbsent(playerId, id -> new GamePlayer(id, safeName(name, "Jogador"), false));
        broadcastState(room);
    }

    public synchronized void leave(String code, String playerId) {
        GameRoom room = rooms.get(code);
        if (room == null) return;
        if (playerId.equals(room.getHostId())) {
            closeRoom(room);
            return;
        }
        room.getPlayers().remove(playerId);
        if (room.getStatus() != RoomStatus.LOBBY && room.allActivePlayersAnswered()) {
            endQuestion(room);
        } else {
            broadcastState(room);
        }
    }

    public synchronized void kick(String code, String hostId, String targetId) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) {
            sendError(code, "Apenas o líder pode remover jogadores.");
            return;
        }
        if (room.getPlayers().remove(targetId) != null) {
            messagingTemplate.convertAndSend(topic(code), new GameEvent(GameEvent.KICKED, targetId));
            broadcastState(room);
        }
    }

    public synchronized void setReady(String code, String playerId, boolean ready) {
        GameRoom room = requireRoom(code);
        GamePlayer player = room.getPlayers().get(playerId);
        if (player != null) {
            player.setReady(ready);
            broadcastState(room);
        }
    }

    public synchronized void pickTeam(String code, String playerId, String teamId) {
        GameRoom room = requireRoom(code);
        GamePlayer player = room.getPlayers().get(playerId);
        if (player != null && room.getTeams().containsKey(teamId)) {
            player.setTeamId(teamId);
            broadcastState(room);
        }
    }

    public synchronized void changeQuiz(String code, String hostId, Long themeId) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) {
            sendError(code, "Apenas o líder pode trocar o quiz.");
            return;
        }
        if (room.getStatus() != RoomStatus.LOBBY) return;
        setTheme(room, themeId);
        broadcastState(room);
    }

    public synchronized void updateConfig(String code, String hostId, GameConfig config) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) {
            sendError(code, "Apenas o líder pode alterar as regras.");
            return;
        }
        if (room.getStatus() != RoomStatus.LOBBY) return;
        GameConfig normalized = config.withDefaults();
        room.setConfig(normalized);
        applyRoomMode(room, normalized.roomMode());
        broadcastState(room);
    }

    public synchronized void chat(String code, String playerId, String content) {
        GameRoom room = requireRoom(code);
        GamePlayer player = room.getPlayers().get(playerId);
        if (player == null || content == null || content.isBlank()) return;
        String text = content.length() > 300 ? content.substring(0, 300) : content;
        ChatMessage message = new ChatMessage(playerId, player.getName(), text, System.currentTimeMillis());
        messagingTemplate.convertAndSend(topic(code), GameEvent.chat(message));
    }

    // ---------- fluxo de jogo ----------

    public synchronized void start(String code, String hostId) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) {
            sendError(code, "Apenas o líder pode iniciar.");
            return;
        }
        if (room.getStatus() != RoomStatus.LOBBY) return;
        if (room.getThemeId() == null) {
            sendError(code, "Selecione um quiz antes de iniciar.");
            return;
        }
        if (!room.allPlayersReady()) {
            sendError(code, "Todos os jogadores precisam estar prontos.");
            return;
        }

        List<Question> questions = loadQuestions(room.getThemeId(), room.getConfig().questionCount());
        if (questions.isEmpty()) {
            sendError(code, "Este quiz não possui questões.");
            return;
        }
        room.setQuestions(questions);
        room.getPlayers().values().forEach(GamePlayer::resetForNewGame);
        startQuestion(room, 0);
    }

    public synchronized void answer(String code, String playerId, Long alternativeId) {
        GameRoom room = requireRoom(code);
        if (room.getStatus() != RoomStatus.IN_QUESTION) return;
        GamePlayer player = room.getPlayers().get(playerId);
        if (player == null || player.isAnsweredCurrent()) return;

        player.setAnsweredCurrent(true);
        player.setCurrentAnswerId(alternativeId);
        player.addScore(computeScore(room, alternativeId));

        if (room.allActivePlayersAnswered()) {
            room.cancelTimer();
            endQuestion(room);
        }
    }

    public synchronized void next(String code, String hostId) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) return;
        if (room.getStatus() != RoomStatus.BETWEEN) return;
        advance(room);
    }

    // ---------- internos ----------

    private void startQuestion(GameRoom room, int index) {
        room.setCurrentIndex(index);
        room.setStatus(RoomStatus.IN_QUESTION);
        room.setQuestionStartMillis(System.currentTimeMillis());
        room.getPlayers().values().forEach(GamePlayer::resetForNewQuestion);

        Question question = room.currentQuestion();
        List<QuestionView.AlternativeView> alternatives = question.getAlternatives().stream()
                .map(a -> new QuestionView.AlternativeView(a.getId(), a.entityToResponse().text()))
                .toList();
        int time = room.getConfig().questionTimeSeconds();
        QuestionView view = new QuestionView(
                question.getId(), question.getTitle(), question.getImageUrl(),
                index, room.getQuestions().size(), time, room.getQuestionStartMillis(), alternatives);

        messagingTemplate.convertAndSend(topic(room.getCode()), GameEvent.question(view));
        broadcastState(room);

        room.setTimerTask(scheduler.schedule(() -> endQuestionSafely(room.getCode(), index),
                time, TimeUnit.SECONDS));
    }

    private void endQuestionSafely(String code, int expectedIndex) {
        synchronized (this) {
            GameRoom room = rooms.get(code);
            if (room == null || room.getStatus() != RoomStatus.IN_QUESTION
                    || room.getCurrentIndex() != expectedIndex) {
                return;
            }
            endQuestion(room);
        }
    }

    private void endQuestion(GameRoom room) {
        room.cancelTimer();
        room.setStatus(RoomStatus.BETWEEN);

        Question question = room.currentQuestion();
        Long correctId = question.getAlternatives().stream()
                .filter(a -> Boolean.TRUE.equals(a.getCorrect()))
                .map(Alternative::getId)
                .findFirst().orElse(null);

        boolean last = room.getCurrentIndex() >= room.getQuestions().size() - 1;
        QuestionResultView result = new QuestionResultView(
                correctId, room.getCurrentIndex(), room.getQuestions().size(), last,
                scoreboard(room), teamScoreboard(room));

        messagingTemplate.convertAndSend(topic(room.getCode()), GameEvent.result(result));
        broadcastState(room);

        if (room.getConfig().advanceMode() == AdvanceMode.AUTO) {
            room.setTimerTask(scheduler.schedule(() -> advanceSafely(room.getCode()),
                    RESULT_DELAY_SECONDS, TimeUnit.SECONDS));
        }
    }

    private void advanceSafely(String code) {
        synchronized (this) {
            GameRoom room = rooms.get(code);
            if (room == null || room.getStatus() != RoomStatus.BETWEEN) return;
            advance(room);
        }
    }

    private void advance(GameRoom room) {
        room.cancelTimer();
        int next = room.getCurrentIndex() + 1;
        if (next >= room.getQuestions().size()) {
            room.setStatus(RoomStatus.FINISHED);
            broadcastState(room);
            return;
        }
        startQuestion(room, next);
    }

    private int computeScore(GameRoom room, Long alternativeId) {
        Question question = room.currentQuestion();
        boolean correct = question.getAlternatives().stream()
                .anyMatch(a -> a.getId().equals(alternativeId) && Boolean.TRUE.equals(a.getCorrect()));
        if (!correct) return 0;
        if (room.getConfig().scoringMode() == ScoringMode.FIXED) return SCORE_MAX;

        long elapsed = System.currentTimeMillis() - room.getQuestionStartMillis();
        double total = room.getConfig().questionTimeSeconds() * 1000.0;
        double fraction = Math.max(0, Math.min(1, elapsed / total));
        return (int) Math.round(SCORE_MAX - (SCORE_MAX - SCORE_MIN_CORRECT) * fraction);
    }

    // ---------- helpers de estado / broadcast ----------

    private void applyRoomMode(GameRoom room, RoomMode mode) {
        if (mode == RoomMode.TEAM) {
            if (room.getTeams().isEmpty()) {
                room.getTeams().put("team-a", new Team("team-a", "Equipe 1"));
                room.getTeams().put("team-b", new Team("team-b", "Equipe 2"));
            }
        } else {
            room.getTeams().clear();
            room.getPlayers().values().forEach(p -> p.setTeamId(null));
        }
    }

    private void setTheme(GameRoom room, Long themeId) {
        Theme theme = themeRepository.findById(themeId).orElse(null);
        room.setThemeId(themeId);
        room.setThemeName(theme != null ? theme.getName() : null);
    }

    private List<Question> loadQuestions(Long themeId, int count) {
        List<Question> all = new ArrayList<>(questionRepository.findByThemeId(themeId));
        Collections.shuffle(all);
        return all.stream().limit(count).toList();
    }

    private void broadcastState(GameRoom room) {
        messagingTemplate.convertAndSend(topic(room.getCode()), GameEvent.state(toState(room)));
    }

    private void closeRoom(GameRoom room) {
        room.cancelTimer();
        rooms.remove(room.getCode());
        messagingTemplate.convertAndSend(topic(room.getCode()), new GameEvent(GameEvent.ROOM_CLOSED, room.getCode()));
    }

    private RoomStateResponse toState(GameRoom room) {
        List<RoomStateResponse.PlayerView> players = room.getPlayers().values().stream()
                .map(p -> new RoomStateResponse.PlayerView(
                        p.getId(), p.getName(), p.isHost(), p.isReady(), p.getTeamId(), p.getScore()))
                .toList();
        return new RoomStateResponse(
                room.getCode(), room.getHostId(), room.getThemeId(), room.getThemeName(),
                room.getConfig(), room.getStatus().name(), players, teamScoreboard(room),
                room.getCurrentIndex(), room.getQuestions().size());
    }

    private List<RoomStateResponse.PlayerView> scoreboard(GameRoom room) {
        return room.getPlayers().values().stream()
                .filter(p -> !p.isHost() || room.getPlayers().size() == 1)
                .sorted(Comparator.comparingInt(GamePlayer::getScore).reversed())
                .map(p -> new RoomStateResponse.PlayerView(
                        p.getId(), p.getName(), p.isHost(), p.isReady(), p.getTeamId(), p.getScore()))
                .toList();
    }

    private List<RoomStateResponse.TeamView> teamScoreboard(GameRoom room) {
        return room.getTeams().values().stream()
                .map(t -> new RoomStateResponse.TeamView(t.getId(), t.getName(),
                        room.getPlayers().values().stream()
                                .filter(p -> t.getId().equals(p.getTeamId()))
                                .mapToInt(GamePlayer::getScore).sum()))
                .toList();
    }

    private GameRoom requireRoom(String code) {
        GameRoom room = rooms.get(code);
        if (room == null) throw new NoSuchElementException("Sala não encontrada: " + code);
        return room;
    }

    private void sendError(String code, String message) {
        messagingTemplate.convertAndSend(topic(code), GameEvent.error(message));
    }

    private String topic(String code) {
        return "/topic/room/" + code;
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }

    private String safeName(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        return name.length() > 30 ? name.substring(0, 30) : name.trim();
    }
}
