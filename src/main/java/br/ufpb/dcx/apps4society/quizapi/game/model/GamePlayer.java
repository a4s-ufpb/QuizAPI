package br.ufpb.dcx.apps4society.quizapi.game.model;

/**
 * Jogador em memória de uma sala multiplayer. Convidado (não exige login);
 * o {@code id} é gerado no cliente. Nada aqui é persistido no banco.
 */
public class GamePlayer {
    private final String id;
    private String name;
    private boolean host;
    private boolean ready;
    private String teamId;
    private int score;
    private int lastGained;
    private boolean answeredCurrent;
    private Long currentAnswerId;
    private String avatar;
    private boolean captain;
    /** UUID da conta real do jogador, se estiver logado (null = convidado). */
    private String userUuid;
    /** Marca de tempo da resposta atual — usado pelo poder "roubar pontos" (quem responde primeiro). */
    private long answeredAtMillis;
    /** Modo Sobrevivência: errou (ou não respondeu) uma questão — vira espectador pro resto da partida. */
    private boolean eliminated;
    /** Cosméticos equipados (só jogadores logados) — puramente visuais no lobby/placares. */
    private String title;
    private String frame;
    private String banner;
    /** Nº de questões que o jogador acertou na partida (para XP/moedas/histórico). */
    private int correctCount;

    public GamePlayer(String id, String name, boolean host) {
        this.id = id;
        this.name = name;
        this.host = host;
    }

    public void resetForNewQuestion() {
        this.answeredCurrent = false;
        this.currentAnswerId = null;
        this.lastGained = 0;
        this.answeredAtMillis = 0;
    }

    public void resetForNewGame() {
        this.score = 0;
        this.lastGained = 0;
        this.answeredCurrent = false;
        this.currentAnswerId = null;
        this.eliminated = false;
        this.correctCount = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; this.lastGained = points; }
    public int getLastGained() { return lastGained; }
    public boolean isAnsweredCurrent() { return answeredCurrent; }
    public void setAnsweredCurrent(boolean answeredCurrent) { this.answeredCurrent = answeredCurrent; }
    public Long getCurrentAnswerId() { return currentAnswerId; }
    public void setCurrentAnswerId(Long currentAnswerId) { this.currentAnswerId = currentAnswerId; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public boolean isCaptain() { return captain; }
    public void setCaptain(boolean captain) { this.captain = captain; }
    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }
    public long getAnsweredAtMillis() { return answeredAtMillis; }
    public void setAnsweredAtMillis(long answeredAtMillis) { this.answeredAtMillis = answeredAtMillis; }
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFrame() { return frame; }
    public void setFrame(String frame) { this.frame = frame; }
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    public int getCorrectCount() { return correctCount; }
    public void incrementCorrectCount() { this.correctCount++; }
}
