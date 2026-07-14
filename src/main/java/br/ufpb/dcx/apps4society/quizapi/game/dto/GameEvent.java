package br.ufpb.dcx.apps4society.quizapi.game.dto;

/**
 * Envelope de todo evento enviado ao tópico {@code /topic/room/{code}}.
 * O cliente faz switch em {@link #type()} para interpretar {@link #data()}.
 */
public record GameEvent(String type, Object data) {
    public static final String STATE = "STATE";
    public static final String QUESTION = "QUESTION";
    public static final String RESULT = "RESULT";
    public static final String CHAT = "CHAT";
    public static final String KICKED = "KICKED";
    public static final String ROOM_CLOSED = "ROOM_CLOSED";
    public static final String ERROR = "ERROR";
    public static final String COUNTDOWN = "COUNTDOWN";

    public static GameEvent state(Object data) { return new GameEvent(STATE, data); }
    public static GameEvent question(Object data) { return new GameEvent(QUESTION, data); }
    public static GameEvent result(Object data) { return new GameEvent(RESULT, data); }
    public static GameEvent chat(Object data) { return new GameEvent(CHAT, data); }
    public static GameEvent error(String message) { return new GameEvent(ERROR, message); }
    /**
     * Erro direcionado a um único jogador. O tópico é compartilhado por toda a
     * sala, então o cliente só exibe o alerta se {@code targetPlayerId} for o
     * seu próprio id (evita, ex., todo mundo ver "A partida já começou" quando
     * um celular reconecta no meio do jogo).
     */
    public static GameEvent errorFor(String targetPlayerId, String message) {
        return new GameEvent(ERROR, new TargetedError(message, targetPlayerId));
    }
    public static GameEvent countdown(int seconds) { return new GameEvent(COUNTDOWN, seconds); }
    public static GameEvent roomClosed(String code) { return new GameEvent(ROOM_CLOSED, code); }

    public record TargetedError(String message, String targetPlayerId) {}
}
