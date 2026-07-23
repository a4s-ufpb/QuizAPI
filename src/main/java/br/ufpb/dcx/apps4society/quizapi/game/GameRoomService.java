package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.Theme;
import br.ufpb.dcx.apps4society.quizapi.game.dto.*;
import br.ufpb.dcx.apps4society.quizapi.game.model.*;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.ThemeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
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
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int SCORE_MAX = 1000;
    private static final int SCORE_MIN_CORRECT = 500;
    private static final int STEAL_AMOUNT = 100;
    private static final long ROOM_IDLE_TIMEOUT_MINUTES = 30;
    private static final long ROOM_SWEEP_INTERVAL_MINUTES = 5;

    private final Map<String, GameRoom> rooms = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    // Dimensionado pelos cores disponíveis: com só 2 threads fixas, salas
    // simultâneas além desse número enfileiram os timers e atrasam o
    // início/fim de questão perceptivelmente para o jogador.
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));

    private final QuestionRepository questionRepository;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public GameRoomService(QuestionRepository questionRepository,
                           ThemeRepository themeRepository,
                           UserRepository userRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.questionRepository = questionRepository;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.scheduler.scheduleAtFixedRate(this::sweepIdleRooms,
                ROOM_SWEEP_INTERVAL_MINUTES, ROOM_SWEEP_INTERVAL_MINUTES, TimeUnit.MINUTES);
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

    /**
     * Cria uma sala de confronto de torneio: gerenciada (a saída do líder não a
     * fecha), com início automático quando {@code autoStartThreshold} jogadores
     * se conectarem. A config já deve vir com hostPlays=true e AdvanceMode.AUTO.
     */
    public synchronized RoomStateResponse createTournamentRoom(CreateRoomRequest request, int autoStartThreshold) {
        RoomStateResponse state = createRoom(request);
        GameRoom room = rooms.get(state.code());
        room.setTournamentManaged(true);
        room.setAutoStartThreshold(autoStartThreshold);
        return toState(room);
    }

    public synchronized RoomStateResponse getState(String code) {
        return toState(requireRoom(code));
    }

    // ---------- lobby ----------

    public synchronized void join(String code, String playerId, String name, String avatar, String userUuid) {
        GameRoom room = requireRoom(code);
        boolean isNew = !room.getPlayers().containsKey(playerId);
        // Quem já está na sala sempre pode "re-entrar" (o cliente STOMP
        // reconecta sozinho após queda de rede e reenvia o join) — em qualquer
        // fase da partida, sem erro. Só jogador NOVO é barrado fora do lobby,
        // e o erro vai direcionado só pra ele (o tópico é da sala inteira).
        if (isNew && room.getStatus() != RoomStatus.LOBBY) {
            sendErrorTo(code, playerId, "A partida já começou.");
            return;
        }
        // Capacidade máxima não conta o host (ele não pontua). Só barra novos
        // jogadores; quem já está na sala (reconexão) sempre passa.
        if (isNew && countNonHostPlayers(room) >= room.getConfig().maxPlayers()) {
            sendErrorTo(code, playerId, "A sala está cheia.");
            return;
        }
        GamePlayer player = room.getPlayers().computeIfAbsent(playerId,
                id -> new GamePlayer(id, safeName(name, "Jogador"), false));
        if (avatar != null) player.setAvatar(avatar);
        if (userUuid != null) {
            player.setUserUuid(userUuid);
            applyCosmetics(player, userUuid);
        }
        room.getConnectedPlayers().add(playerId);
        broadcastState(room);
        maybeAutoStart(room);
    }

    /**
     * Salas de torneio começam sozinhas: quando o número esperado de
     * adversários já se conectou, dispara a contagem regressiva e a 1ª questão
     * sem depender de nenhum líder clicar em "iniciar".
     */
    private void maybeAutoStart(GameRoom room) {
        if (room.getAutoStartThreshold() <= 0) return;
        if (room.getStatus() != RoomStatus.LOBBY) return;
        if (room.getConnectedPlayers().size() < room.getAutoStartThreshold()) return;
        startGame(room);
    }

    private int countNonHostPlayers(GameRoom room) {
        return (int) room.getPlayers().values().stream().filter(p -> !p.isHost()).count();
    }

    /** Copia os cosméticos equipados da conta real pro jogador da sala (só visual). */
    private void applyCosmetics(GamePlayer player, String userUuid) {
        try {
            User user = userRepository.findById(UUID.fromString(userUuid)).orElse(null);
            if (user == null) return;
            player.setTitle(user.getEquippedTitle());
            player.setFrame(user.getEquippedFrame());
            player.setBanner(user.getEquippedBanner());
            player.setFont(user.getEquippedFont());
            player.setNameStyle(user.getEquippedNameStyle());
            player.setNameEffect(user.getEquippedNameEffect());
        } catch (IllegalArgumentException ignored) {
            // userUuid não é um UUID válido (ex.: convidado) — sem cosméticos.
        }
    }

    public synchronized void leave(String code, String playerId) {
        GameRoom room = rooms.get(code);
        if (room == null) return;
        room.getConnectedPlayers().remove(playerId);
        // Numa sala de torneio o líder é um jogador comum do confronto: sua saída
        // NÃO encerra a sala (o torneio ainda precisa apurar o vencedor pelo
        // estado final). A sala é recolhida depois pela varredura de ociosas.
        if (playerId.equals(room.getHostId()) && !room.isTournamentManaged()) {
            closeRoom(room);
            return;
        }
        GamePlayer leaving = room.getPlayers().remove(playerId);
        if (leaving != null && leaving.isCaptain() && leaving.getTeamId() != null) {
            promoteNextCaptain(room, leaving.getTeamId());
        }
        if (room.getStatus() != RoomStatus.LOBBY && room.allActivePlayersAnswered()) {
            endQuestion(room);
        } else {
            broadcastState(room);
        }
    }

    /** Promove o próximo membro (por ordem de entrada) da equipe a capitão, ou limpa se ficou vazia. */
    private void promoteNextCaptain(GameRoom room, String teamId) {
        Team team = room.getTeams().get(teamId);
        if (team == null) return;
        GamePlayer next = room.getPlayers().values().stream()
                .filter(p -> teamId.equals(p.getTeamId()))
                .findFirst()
                .orElse(null);
        if (next == null) {
            team.setCaptainId(null);
        } else {
            team.setCaptainId(next.getId());
            next.setCaptain(true);
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
        Team team = room.getTeams().get(teamId);
        if (player == null || team == null) return;

        String previousTeamId = player.getTeamId();
        boolean wasCaptainOfPrevious = player.isCaptain();
        player.setTeamId(teamId);
        player.setCaptain(false);

        if (previousTeamId != null && wasCaptainOfPrevious && !previousTeamId.equals(teamId)) {
            promoteNextCaptain(room, previousTeamId);
        }

        // Primeiro jogador a entrar na equipe vira capitão.
        if (team.getCaptainId() == null) {
            team.setCaptainId(playerId);
            player.setCaptain(true);
        }
        broadcastState(room);
    }

    public synchronized void createTeam(String code, String playerId, String teamName) {
        GameRoom room = requireRoom(code);
        GamePlayer player = room.getPlayers().get(playerId);
        if (player == null || room.getConfig().roomMode() != RoomMode.TEAM) return;
        if (teamName == null || teamName.isBlank()) return;

        String previousTeamId = player.getTeamId();
        boolean wasCaptainOfPrevious = player.isCaptain();

        String teamId = "team-" + UUID.randomUUID().toString().substring(0, 8);
        Team team = new Team(teamId, teamName.trim());
        team.setCaptainId(playerId);
        room.getTeams().put(teamId, team);
        player.setTeamId(teamId);
        player.setCaptain(true);

        if (previousTeamId != null && wasCaptainOfPrevious) {
            promoteNextCaptain(room, previousTeamId);
        }
        broadcastState(room);
    }

    public synchronized void transferCaptain(String code, String playerId, String teamId, String newCaptainId) {
        GameRoom room = requireRoom(code);
        Team team = room.getTeams().get(teamId);
        GamePlayer currentCaptain = room.getPlayers().get(playerId);
        GamePlayer newCaptain = room.getPlayers().get(newCaptainId);
        if (team == null || currentCaptain == null || newCaptain == null) return;
        if (!playerId.equals(team.getCaptainId())) return;
        if (!teamId.equals(newCaptain.getTeamId())) return;

        currentCaptain.setCaptain(false);
        newCaptain.setCaptain(true);
        team.setCaptainId(newCaptainId);
        broadcastState(room);
    }

    public synchronized void setAvatar(String code, String playerId, String avatar) {
        GameRoom room = requireRoom(code);
        GamePlayer player = room.getPlayers().get(playerId);
        if (player == null) return;
        player.setAvatar(avatar);
        broadcastState(room);
    }

    public synchronized void setTeamAvatar(String code, String playerId, String teamId, String avatar) {
        GameRoom room = requireRoom(code);
        GamePlayer player = room.getPlayers().get(playerId);
        Team team = room.getTeams().get(teamId);
        if (player == null || team == null || !teamId.equals(player.getTeamId())) return;
        team.setAvatar(avatar);
        broadcastState(room);
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
        // O líder pode iniciar mesmo sem todos prontos — o front exibe uma
        // confirmação antes de enviar o start nesse caso.
        startGame(room);
    }

    /**
     * Inicia a partida (contagem + 1ª questão). Usado tanto pelo comando manual
     * do líder quanto pelo auto-início das salas de torneio. Não valida líder —
     * quem chama é responsável por isso.
     */
    private void startGame(GameRoom room) {
        if (room.getStatus() != RoomStatus.LOBBY) return;
        if (room.getThemeId() == null) {
            sendError(room.getCode(), "Selecione um quiz antes de iniciar.");
            return;
        }
        List<Question> questions = loadQuestions(room.getThemeId(), room.getConfig().questionCount());
        if (questions.isEmpty()) {
            sendError(room.getCode(), "Este quiz não possui questões.");
            return;
        }
        room.setQuestions(questions);
        room.getPlayers().values().forEach(GamePlayer::resetForNewGame);

        messagingTemplate.convertAndSend(topic(room.getCode()), GameEvent.countdown(COUNTDOWN_SECONDS));
        scheduler.schedule(() -> startFirstQuestionSafely(room.getCode()), COUNTDOWN_SECONDS, TimeUnit.SECONDS);
    }

    private void startFirstQuestionSafely(String code) {
        synchronized (this) {
            GameRoom room = rooms.get(code);
            if (room == null || room.getStatus() != RoomStatus.LOBBY) return;
            room.touch();
            startQuestion(room, 0);
        }
    }

    public synchronized void answer(String code, String playerId, Long alternativeId) {
        GameRoom room = requireRoom(code);
        if (room.getStatus() != RoomStatus.IN_QUESTION) return;
        GamePlayer player = room.getPlayers().get(playerId);
        if (player == null || player.isAnsweredCurrent()) return;
        // Líder espectador (não conta) não responde; só participantes que pontuam.
        if (!room.counts(player)) return;
        if (room.getConfig().roomMode() == RoomMode.TEAM && !player.isCaptain()) return;
        if (player.isEliminated()) return;

        player.setAnsweredCurrent(true);
        player.setCurrentAnswerId(alternativeId);
        player.setAnsweredAtMillis(System.currentTimeMillis());
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

    /**
     * Líder descarta a próxima questão sem jogá-la. O índice atual anda 1
     * posição "em seco" e apenas o STATE é reemitido (com a nova prévia da
     * próxima questão) — o RESULT NÃO é reenviado, para não re-disparar a
     * animação de revelação nos clientes. A questão pulada nunca é enviada como
     * QUESTION, então os jogadores não a veem nem a respondem; ela só aparece
     * quando o líder confirma "Próxima questão".
     */
    public synchronized void skipNextQuestion(String code, String hostId) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) return;
        if (room.getStatus() != RoomStatus.BETWEEN) return;
        if (room.getLastResult() == null) return;
        // Não há próxima questão pra pular.
        if (room.getCurrentIndex() + 1 >= room.getQuestions().size()) return;

        room.setCurrentIndex(room.getCurrentIndex() + 1);
        room.setSkippedCount(room.getSkippedCount() + 1);
        broadcastState(room);
    }

    /**
     * Líder escolhe (ou limpa, com {@code power == null}) o poder que vai
     * valer pra próxima questão — só no modo Diversão, só entre questões.
     */
    public synchronized void setPendingPower(String code, String hostId, String power) {
        GameRoom room = requireRoom(code);
        if (!hostId.equals(room.getHostId())) return;
        if (room.getConfig().gameStyle() != GameStyle.FUN) return;
        if (room.getStatus() != RoomStatus.BETWEEN) return;

        QuestionPower parsed = parsePower(power);
        room.setPendingPowerUp(parsed);
        broadcastState(room);
    }

    private QuestionPower parsePower(String power) {
        if (power == null || power.isBlank()) return null;
        try {
            return QuestionPower.valueOf(power);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------- internos ----------

    private void startQuestion(GameRoom room, int index) {
        room.setCurrentIndex(index);
        room.setStatus(RoomStatus.IN_QUESTION);
        room.setQuestionStartMillis(System.currentTimeMillis());
        room.setSkippedCount(0);
        room.getPlayers().values().forEach(GamePlayer::resetForNewQuestion);

        // O poder armado pelo líder passa a valer pra essa questão e é
        // consumido — pra continuar valendo ele precisa escolher de novo.
        QuestionPower power = room.getPendingPowerUp();
        room.setPendingPowerUp(null);
        room.setCurrentPowerUp(power);

        Question question = room.currentQuestion();
        List<QuestionView.AlternativeView> alternatives = question.getAlternatives().stream()
                .map(a -> new QuestionView.AlternativeView(a.getId(), a.entityToResponse().text()))
                .toList();
        if (power == QuestionPower.HIDE_WRONG_ALTERNATIVE) {
            alternatives = hideOneWrongAlternative(question, alternatives);
        }

        int time = effectiveQuestionTime(room, index);
        QuestionView view = new QuestionView(
                question.getId(), question.getTitle(), question.getImageUrl(),
                question.getImageOneUrl(), question.getImageTwoUrl(),
                question.getImagesOrder(),
                index, room.getQuestions().size(), time, room.getQuestionStartMillis(), alternatives,
                power != null ? power.name() : null);

        messagingTemplate.convertAndSend(topic(room.getCode()), GameEvent.question(view));
        broadcastState(room);

        room.setTimerTask(scheduler.schedule(() -> endQuestionSafely(room.getCode(), index),
                time, TimeUnit.SECONDS));
    }

    // Modo Relâmpago: 5s a menos por questão (nunca abaixo de 5s no total).
    private static final int LIGHTNING_TIME_STEP_SECONDS = 5;
    private static final int LIGHTNING_MIN_TIME_SECONDS = 5;

    private int effectiveQuestionTime(GameRoom room, int index) {
        int base = room.getConfig().questionTimeSeconds();
        if (room.getConfig().gameStyle() != GameStyle.LIGHTNING) return base;
        int reduced = base - (index * LIGHTNING_TIME_STEP_SECONDS);
        return Math.max(LIGHTNING_MIN_TIME_SECONDS, reduced);
    }

    /** Remove (determinístico, sempre a mesma) uma alternativa incorreta da lista enviada aos jogadores. */
    private List<QuestionView.AlternativeView> hideOneWrongAlternative(Question question, List<QuestionView.AlternativeView> alternatives) {
        Long wrongId = question.getAlternatives().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getCorrect()))
                .map(Alternative::getId)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (wrongId == null) return alternatives;
        return alternatives.stream().filter(a -> !a.id().equals(wrongId)).toList();
    }

    private void endQuestionSafely(String code, int expectedIndex) {
        synchronized (this) {
            GameRoom room = rooms.get(code);
            if (room == null || room.getStatus() != RoomStatus.IN_QUESTION
                    || room.getCurrentIndex() != expectedIndex) {
                return;
            }
            room.touch();
            endQuestion(room);
        }
    }

    private void endQuestion(GameRoom room) {
        room.cancelTimer();
        room.setStatus(RoomStatus.BETWEEN);
        room.setSkippedCount(0);

        Question question = room.currentQuestion();
        Long correctId = question.getAlternatives().stream()
                .filter(a -> Boolean.TRUE.equals(a.getCorrect()))
                .map(Alternative::getId)
                .findFirst().orElse(null);

        // Conta acertos por jogador (base de XP/moedas/histórico) — independente
        // da pontuação por velocidade.
        if (correctId != null) {
            room.getPlayers().values().forEach(p -> {
                if (correctId.equals(p.getCurrentAnswerId())) p.incrementCorrectCount();
            });
        }

        if (room.getCurrentPowerUp() == QuestionPower.STEAL_POINTS) {
            applyStealPoints(room, correctId);
        }
        if (room.getConfig().gameStyle() == GameStyle.SURVIVAL) {
            applySurvivalElimination(room, correctId);
        }

        boolean last = room.getCurrentIndex() >= room.getQuestions().size() - 1
                || room.survivalShouldEndEarly();
        QuestionResultView result = new QuestionResultView(
                correctId, room.getCurrentIndex(), room.getQuestions().size(), last,
                scoreboard(room), teamScoreboard(room), playerAnswers(room, correctId),
                alternativeCounts(room, question), nextQuestionTitle(room, last));
        room.setLastResult(result);

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
            room.touch();
            advance(room);
        }
    }

    private void advance(GameRoom room) {
        room.cancelTimer();
        int next = room.getCurrentIndex() + 1;
        if (next >= room.getQuestions().size() || room.survivalShouldEndEarly()) {
            room.setStatus(RoomStatus.FINISHED);
            broadcastState(room);
            return;
        }
        startQuestion(room, next);
    }

    /**
     * Modo Sobrevivência: quem errou (ou não respondeu) essa questão é
     * eliminado. Se todos os ativos errassem ao mesmo tempo ninguém sobraria
     * pra próxima rodada — nesse caso ninguém é eliminado (rodada "de graça").
     */
    private void applySurvivalElimination(GameRoom room, Long correctId) {
        List<GamePlayer> active = room.getPlayers().values().stream()
                .filter(room::counts)
                .filter(p -> !p.isEliminated())
                .toList();

        boolean anySurvivor = correctId != null && active.stream()
                .anyMatch(p -> correctId.equals(p.getCurrentAnswerId()));
        if (!anySurvivor) return;

        for (GamePlayer p : active) {
            boolean correct = correctId != null && correctId.equals(p.getCurrentAnswerId());
            if (!correct) p.setEliminated(true);
        }
    }

    private int computeScore(GameRoom room, Long alternativeId) {
        Question question = room.currentQuestion();
        boolean correct = question.getAlternatives().stream()
                .anyMatch(a -> a.getId().equals(alternativeId) && Boolean.TRUE.equals(a.getCorrect()));
        if (!correct) return 0;

        int base;
        if (room.getConfig().scoringMode() == ScoringMode.FIXED) {
            base = SCORE_MAX;
        } else {
            long elapsed = System.currentTimeMillis() - room.getQuestionStartMillis();
            double total = effectiveQuestionTime(room, room.getCurrentIndex()) * 1000.0;
            double fraction = Math.max(0, Math.min(1, elapsed / total));
            base = (int) Math.round(SCORE_MAX - (SCORE_MAX - SCORE_MIN_CORRECT) * fraction);
        }

        QuestionPower power = room.getCurrentPowerUp();
        if (power == null) return base;
        return (int) Math.round(base * power.scoreMultiplier());
    }

    /** Poder "roubar pontos": quem responder certo primeiro rouba STEAL_AMOUNT de cada outro jogador ativo. */
    private void applyStealPoints(GameRoom room, Long correctId) {
        if (correctId == null) return;
        GamePlayer thief = room.getPlayers().values().stream()
                .filter(room::counts)
                .filter(GamePlayer::isAnsweredCurrent)
                .filter(p -> correctId.equals(p.getCurrentAnswerId()))
                .min(Comparator.comparingLong(GamePlayer::getAnsweredAtMillis))
                .orElse(null);
        if (thief == null) return;

        int stolenTotal = 0;
        for (GamePlayer p : room.getPlayers().values()) {
            if (p == thief) continue;
            if (!room.counts(p)) continue;
            int steal = Math.min(STEAL_AMOUNT, Math.max(0, p.getScore()));
            p.addScore(-steal);
            stolenTotal += steal;
        }
        thief.addScore(stolenTotal);
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
        room.setThemeImageUrl(theme != null ? theme.getImageUrl() : null);
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
                        p.getId(), p.getName(), p.isHost(), p.isReady(), p.getTeamId(), p.getScore(), p.getAvatar(), p.isCaptain(), p.getUserUuid(), p.isEliminated(),
                        p.getTitle(), p.getFrame(), p.getBanner(), p.getFont(), p.getNameStyle(), p.getNameEffect(), p.getCorrectCount()))
                .toList();
        return new RoomStateResponse(
                room.getCode(), room.getHostId(), room.getThemeId(), room.getThemeName(), room.getThemeImageUrl(),
                room.getConfig(), room.getStatus().name(), players, teamScoreboard(room),
                room.getCurrentIndex(), room.getQuestions().size(),
                room.getPendingPowerUp() != null ? room.getPendingPowerUp().name() : null,
                nextQuestionPreview(room), room.getSkippedCount());
    }

    /**
     * Prévia da próxima questão para o líder pré-visualizar/pular — só entre
     * questões (BETWEEN) e quando ainda há próxima (não é o resultado final).
     */
    private RoomStateResponse.NextQuestionPreview nextQuestionPreview(GameRoom room) {
        if (room.getStatus() != RoomStatus.BETWEEN) return null;
        if (room.getLastResult() != null && room.getLastResult().lastQuestion()) return null;
        int next = room.getCurrentIndex() + 1;
        if (next < 0 || next >= room.getQuestions().size()) return null;

        Question q = room.getQuestions().get(next);
        List<RoomStateResponse.NextQuestionPreview.AlternativePreview> alternatives =
                q.getAlternatives().stream()
                        .map(a -> new RoomStateResponse.NextQuestionPreview.AlternativePreview(
                                a.getId(), a.entityToResponse().text()))
                        .toList();
        return new RoomStateResponse.NextQuestionPreview(
                q.getId(), q.getTitle(), q.getImageUrl(), q.getImageOneUrl(), q.getImageTwoUrl(),
                q.getImagesOrder(), alternatives);
    }

    private List<RoomStateResponse.PlayerView> scoreboard(GameRoom room) {
        return room.getPlayers().values().stream()
                .filter(room::counts)
                .sorted(Comparator.comparingInt(GamePlayer::getScore).reversed())
                .map(p -> new RoomStateResponse.PlayerView(
                        p.getId(), p.getName(), p.isHost(), p.isReady(), p.getTeamId(), p.getScore(), p.getAvatar(), p.isCaptain(), p.getUserUuid(), p.isEliminated(),
                        p.getTitle(), p.getFrame(), p.getBanner(), p.getFont(), p.getNameStyle(), p.getNameEffect(), p.getCorrectCount()))
                .toList();
    }

    /** Distribuição de respostas por alternativa da questão encerrada (host não conta, exceto jogando sozinho). */
    private List<QuestionResultView.AlternativeCountView> alternativeCounts(GameRoom room, Question question) {
        List<GamePlayer> answering = room.getPlayers().values().stream()
                .filter(room::counts)
                .toList();
        // Mesma ordem enviada no QuestionView — o front usa a posição pra manter
        // as cores das barras iguais às dos botões de alternativa.
        return question.getAlternatives().stream()
                .map(a -> new QuestionResultView.AlternativeCountView(
                        a.getId(),
                        a.entityToResponse().text(),
                        (int) answering.stream()
                                .filter(p -> a.getId().equals(p.getCurrentAnswerId()))
                                .count(),
                        Boolean.TRUE.equals(a.getCorrect())))
                .toList();
    }

    private String nextQuestionTitle(GameRoom room, boolean last) {
        if (last) return null;
        int next = room.getCurrentIndex() + 1;
        if (next >= room.getQuestions().size()) return null;
        return room.getQuestions().get(next).getTitle();
    }

    private List<QuestionResultView.PlayerAnswerView> playerAnswers(GameRoom room, Long correctId) {
        return room.getPlayers().values().stream()
                .filter(room::counts)
                .map(p -> new QuestionResultView.PlayerAnswerView(
                        p.getId(), p.getName(), p.isAnsweredCurrent(),
                        p.getCurrentAnswerId() != null && p.getCurrentAnswerId().equals(correctId)))
                .toList();
    }

    private List<RoomStateResponse.TeamView> teamScoreboard(GameRoom room) {
        return room.getTeams().values().stream()
                .map(t -> new RoomStateResponse.TeamView(t.getId(), t.getName(),
                        room.getPlayers().values().stream()
                                .filter(p -> t.getId().equals(p.getTeamId()))
                                .mapToInt(GamePlayer::getScore).sum(),
                        t.getAvatar(), t.getCaptainId()))
                .toList();
    }

    private GameRoom requireRoom(String code) {
        GameRoom room = rooms.get(code);
        if (room == null) throw new NoSuchElementException("Sala não encontrada: " + code);
        room.touch();
        return room;
    }

    /**
     * Remove periodicamente salas sem nenhuma ação de jogador há mais de
     * ROOM_IDLE_TIMEOUT_MINUTES — evita que salas abandonadas (host caiu sem
     * fechar) fiquem ocupando memória indefinidamente.
     */
    private void sweepIdleRooms() {
        long cutoff = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(ROOM_IDLE_TIMEOUT_MINUTES);
        synchronized (this) {
            rooms.entrySet().removeIf(entry -> {
                GameRoom room = entry.getValue();
                boolean idle = room.getLastActivityMillis() < cutoff;
                if (idle) {
                    room.cancelTimer();
                    messagingTemplate.convertAndSend(topic(entry.getKey()), GameEvent.roomClosed(entry.getKey()));
                }
                return idle;
            });
        }
    }

    private void sendError(String code, String message) {
        messagingTemplate.convertAndSend(topic(code), GameEvent.error(message));
    }

    /** Erro exibido apenas pelo jogador alvo (o tópico é compartilhado pela sala inteira). */
    private void sendErrorTo(String code, String targetPlayerId, String message) {
        messagingTemplate.convertAndSend(topic(code), GameEvent.errorFor(targetPlayerId, message));
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
