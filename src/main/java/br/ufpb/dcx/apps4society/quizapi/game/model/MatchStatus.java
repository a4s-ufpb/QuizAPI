package br.ufpb.dcx.apps4society.quizapi.game.model;

public enum MatchStatus {
    // BYE: só 1 jogador no confronto, avança automático (chaveamento não é potência de 2).
    PENDING, WAITING_PLAYERS, IN_PROGRESS, DONE, BYE
}
