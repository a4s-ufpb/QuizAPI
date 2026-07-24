package br.ufpb.dcx.apps4society.quizapi.game.model;

public enum TournamentStatus {
    /** Aguardando jogadores; permite entrada e saída livre. */
    LOBBY,
    /** Chaves travadas (nº de jogadores fixado em 4/8/16); o organizador escolhe o quiz de cada rodada. Ninguém entra. */
    CONFIGURING,
    IN_PROGRESS,
    FINISHED
}
