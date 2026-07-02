package br.ufpb.dcx.apps4society.quizapi.game.dto;

import br.ufpb.dcx.apps4society.quizapi.game.model.AdvanceMode;
import br.ufpb.dcx.apps4society.quizapi.game.model.RoomMode;
import br.ufpb.dcx.apps4society.quizapi.game.model.ScoringMode;

/**
 * Configuração das regras de uma partida multiplayer. Todos os campos têm
 * default seguro via {@link #withDefaults()} para permitir criação de sala sem
 * configuração explícita.
 */
public record GameConfig(
        RoomMode roomMode,
        ScoringMode scoringMode,
        AdvanceMode advanceMode,
        Integer questionTimeSeconds,
        Integer questionCount
) {
    public static GameConfig defaults() {
        return new GameConfig(RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.HOST, 20, 10);
    }

    /** Preenche campos nulos com os defaults e aplica limites saudáveis. */
    public GameConfig withDefaults() {
        GameConfig d = defaults();
        int time = questionTimeSeconds != null ? questionTimeSeconds : d.questionTimeSeconds();
        int count = questionCount != null ? questionCount : d.questionCount();
        time = Math.max(5, Math.min(120, time));
        count = Math.max(1, Math.min(30, count));
        return new GameConfig(
                roomMode != null ? roomMode : d.roomMode(),
                scoringMode != null ? scoringMode : d.scoringMode(),
                advanceMode != null ? advanceMode : d.advanceMode(),
                time,
                count
        );
    }
}
