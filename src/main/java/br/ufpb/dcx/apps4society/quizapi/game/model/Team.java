package br.ufpb.dcx.apps4society.quizapi.game.model;

/** Equipe em memória (usada apenas no modo TEAM). */
public class Team {
    private final String id;
    private String name;

    public Team(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
