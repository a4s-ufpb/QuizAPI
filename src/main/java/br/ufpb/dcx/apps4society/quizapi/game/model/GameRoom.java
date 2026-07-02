package br.ufpb.dcx.apps4society.quizapi.game.model;

import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Estado completo de uma sala multiplayer, mantido apenas em memória.
 * As respostas e a pontuação nunca são persistidas no banco.
 */
public class GameRoom {
    private final String code;
    private final String hostId;
    private GameConfig config;
    private Long themeId;
    private String themeName;
    private RoomStatus status = RoomStatus.LOBBY;

    private final Map<String, GamePlayer> players = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();

    /** Snapshot das questões (entidades destacadas) carregado no start. */
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = -1;
    private long questionStartMillis;

    /** Timer agendado (fim de questão / avanço automático) para permitir cancelamento. */
    private ScheduledFuture<?> timerTask;

    public GameRoom(String code, String hostId, GameConfig config) {
        this.code = code;
        this.hostId = hostId;
        this.config = config;
    }

    public Question currentQuestion() {
        if (currentIndex < 0 || currentIndex >= questions.size()) return null;
        return questions.get(currentIndex);
    }

    public boolean allActivePlayersAnswered() {
        return players.values().stream()
                .filter(p -> !p.isHost() || players.size() == 1)
                .allMatch(GamePlayer::isAnsweredCurrent);
    }

    public boolean allPlayersReady() {
        return players.values().stream()
                .filter(p -> !p.isHost())
                .allMatch(GamePlayer::isReady);
    }

    public void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel(false);
            timerTask = null;
        }
    }

    public String getCode() { return code; }
    public String getHostId() { return hostId; }
    public GameConfig getConfig() { return config; }
    public void setConfig(GameConfig config) { this.config = config; }
    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public String getThemeName() { return themeName; }
    public void setThemeName(String themeName) { this.themeName = themeName; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public Map<String, GamePlayer> getPlayers() { return players; }
    public Map<String, Team> getTeams() { return teams; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
    public long getQuestionStartMillis() { return questionStartMillis; }
    public void setQuestionStartMillis(long questionStartMillis) { this.questionStartMillis = questionStartMillis; }
    public void setTimerTask(ScheduledFuture<?> timerTask) { this.timerTask = timerTask; }
}
