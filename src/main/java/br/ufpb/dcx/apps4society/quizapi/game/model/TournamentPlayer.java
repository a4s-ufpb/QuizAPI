package br.ufpb.dcx.apps4society.quizapi.game.model;

public class TournamentPlayer {
    private final String id;
    private String name;
    private final boolean host;
    private boolean eliminated;
    /** Cosméticos equipados (só jogadores logados) — puramente visuais. */
    private String title;
    private String frame;
    private String banner;

    public TournamentPlayer(String id, String name, boolean host) {
        this.id = id;
        this.name = name;
        this.host = host;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isHost() { return host; }
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFrame() { return frame; }
    public void setFrame(String frame) { this.frame = frame; }
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
}
