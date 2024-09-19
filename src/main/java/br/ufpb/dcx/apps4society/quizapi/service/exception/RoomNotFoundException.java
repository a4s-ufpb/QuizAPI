package br.ufpb.dcx.apps4society.quizapi.service.exception;

public class RoomNotFoundException extends RuntimeException{
    public RoomNotFoundException(String message) {
        super(message);
    }
}
