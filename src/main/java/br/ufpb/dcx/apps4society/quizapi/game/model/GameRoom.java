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
    private String themeImageUrl;
    private RoomStatus status = RoomStatus.LOBBY;

    private final Map<String, GamePlayer> players = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();

    /** Snapshot das questões (entidades destacadas) carregado no start. */
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = -1;
    private long questionStartMillis;

    /** Timer agendado (fim de questão / avanço automático) para permitir cancelamento. */
    private ScheduledFuture<?> timerTask;

    /** Marca de tempo da última ação de qualquer jogador, usada pra varredura de salas abandonadas. */
    private long lastActivityMillis = System.currentTimeMillis();

    /** Poder escolhido pelo líder (modo Diversão), ainda não aplicado — vale pra próxima questão. */
    private QuestionPower pendingPowerUp;
    /** Poder que está valendo pra questão atual (aplicado no start dela). */
    private QuestionPower currentPowerUp;

    public GameRoom(String code, String hostId, GameConfig config) {
        this.code = code;
        this.hostId = hostId;
        this.config = config;
    }

    public void touch() {
        this.lastActivityMillis = System.currentTimeMillis();
    }

    public long getLastActivityMillis() {
        return lastActivityMillis;
    }

    public Question currentQuestion() {
        if (currentIndex < 0 || currentIndex >= questions.size()) return null;
        return questions.get(currentIndex);
    }

    public boolean allActivePlayersAnswered() {
        if (config.roomMode() == RoomMode.TEAM) {
            return teams.values().stream()
                    .filter(t -> players.values().stream().anyMatch(p -> t.getId().equals(p.getTeamId())))
                    .allMatch(t -> {
                        GamePlayer captain = players.get(t.getCaptainId());
                        return captain != null && captain.isAnsweredCurrent();
                    });
        }
        return players.values().stream()
                .filter(p -> !p.isHost() || players.size() == 1)
                .filter(p -> !p.isEliminated())
                .allMatch(GamePlayer::isAnsweredCurrent);
    }

    /** Sobrevivência: só termina cedo quando restar no máximo 1 jogador de pé (evita ficar travado esperando). */
    public boolean survivalShouldEndEarly() {
        if (config.gameStyle() != GameStyle.SURVIVAL) return false;
        boolean soloHost = players.size() == 1;
        long remaining = players.values().stream()
                .filter(p -> !p.isHost() || soloHost)
                .filter(p -> !p.isEliminated())
                .count();
        return remaining <= 1;
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
    public String getThemeImageUrl() { return themeImageUrl; }
    public void setThemeImageUrl(String themeImageUrl) { this.themeImageUrl = themeImageUrl; }
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
    public QuestionPower getPendingPowerUp() { return pendingPowerUp; }
    public void setPendingPowerUp(QuestionPower pendingPowerUp) { this.pendingPowerUp = pendingPowerUp; }
    public QuestionPower getCurrentPowerUp() { return currentPowerUp; }
    public void setCurrentPowerUp(QuestionPower currentPowerUp) { this.currentPowerUp = currentPowerUp; }
}
