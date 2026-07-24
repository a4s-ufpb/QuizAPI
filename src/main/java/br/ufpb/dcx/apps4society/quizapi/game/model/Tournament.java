package br.ufpb.dcx.apps4society.quizapi.game.model;

import br.ufpb.dcx.apps4society.quizapi.game.dto.GameConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Torneio eliminatório em memória (mesmo padrão do {@code GameRoom} — nada
 * aqui é persistido). Cada confronto do chaveamento vira uma sala normal de
 * multiplayer (reaproveitando {@code GameRoomService}); o torneio só orquestra
 * quem joga com quem e avança o vencedor pra próxima rodada.
 */
public class Tournament {
    private final String code;
    private final String hostId;
    private String name;
    private Long themeId;
    private String themeName;
    private GameConfig matchConfig;
    private TournamentStatus status = TournamentStatus.LOBBY;

    private final Map<String, TournamentPlayer> players = new LinkedHashMap<>();
    /** rounds.get(0) = oitavas/primeira rodada, etc. */
    private final List<List<Match>> rounds = new ArrayList<>();
    private String championId;
    /** Capacidade máxima. Default 12; definido na criação. */
    private int maxPlayers = 12;

    /**
     * Quiz escolhido pelo organizador para cada rodada do chaveamento (índice 0 =
     * 1ª rodada). Dimensionado ao travar as chaves (fase CONFIGURING); cada
     * confronto daquela rodada usa esse tema. null = ainda não escolhido.
     */
    private final List<Long> roundThemeIds = new ArrayList<>();
    private final List<String> roundThemeNames = new ArrayList<>();
    /** Nº de jogadores fixado ao travar as chaves (4, 8 ou 16). */
    private int bracketSize;

    private long lastActivityMillis = System.currentTimeMillis();

    public Tournament(String code, String hostId, String name, Long themeId, GameConfig matchConfig) {
        this.code = code;
        this.hostId = hostId;
        this.name = name;
        this.themeId = themeId;
        this.matchConfig = matchConfig;
    }

    public void touch() { this.lastActivityMillis = System.currentTimeMillis(); }
    public long getLastActivityMillis() { return lastActivityMillis; }

    public String getCode() { return code; }
    public String getHostId() { return hostId; }
    public String getName() { return name; }
    public Long getThemeId() { return themeId; }
    public String getThemeName() { return themeName; }
    public void setThemeName(String themeName) { this.themeName = themeName; }
    public GameConfig getMatchConfig() { return matchConfig; }
    public TournamentStatus getStatus() { return status; }
    public void setStatus(TournamentStatus status) { this.status = status; }
    public Map<String, TournamentPlayer> getPlayers() { return players; }
    public List<List<Match>> getRounds() { return rounds; }
    public String getChampionId() { return championId; }
    public void setChampionId(String championId) { this.championId = championId; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public List<Long> getRoundThemeIds() { return roundThemeIds; }
    public List<String> getRoundThemeNames() { return roundThemeNames; }
    public int getBracketSize() { return bracketSize; }
    public void setBracketSize(int bracketSize) { this.bracketSize = bracketSize; }
}
