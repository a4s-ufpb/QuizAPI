package br.ufpb.dcx.apps4society.quizapi.game.model;

public class Match {
    private final String id;
    private String player1Id;
    private String player2Id;
    private String winnerId;
    private String roomCode;
    private MatchStatus status;

    public Match(String id, String player1Id, String player2Id) {
        this.id = id;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.status = (player1Id == null || player2Id == null) ? MatchStatus.BYE : MatchStatus.PENDING;
        // Bye: quem sobrou já avança automaticamente.
        if (this.status == MatchStatus.BYE) {
            this.winnerId = player1Id != null ? player1Id : player2Id;
        }
    }

    public String getId() { return id; }
    public String getPlayer1Id() { return player1Id; }
    public void setPlayer1Id(String player1Id) { this.player1Id = player1Id; }
    public String getPlayer2Id() { return player2Id; }
    public void setPlayer2Id(String player2Id) { this.player2Id = player2Id; }
    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }
}
