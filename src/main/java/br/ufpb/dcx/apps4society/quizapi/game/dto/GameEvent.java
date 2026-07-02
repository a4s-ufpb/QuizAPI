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

    public static GameEvent state(Object data) { return new GameEvent(STATE, data); }
    public static GameEvent question(Object data) { return new GameEvent(QUESTION, data); }
    public static GameEvent result(Object data) { return new GameEvent(RESULT, data); }
    public static GameEvent chat(Object data) { return new GameEvent(CHAT, data); }
    public static GameEvent error(String message) { return new GameEvent(ERROR, message); }
}
