package br.ufpb.dcx.apps4society.quizapi.game.dto;

import br.ufpb.dcx.apps4society.quizapi.game.model.AdvanceMode;
import br.ufpb.dcx.apps4society.quizapi.game.model.GameStyle;
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
        Integer questionCount,
        Integer maxPlayersPerTeam,
        GameStyle gameStyle,
        Integer maxPlayers
) {
    // Capacidade máxima da sala (não conta o host). O valor efetivo permitido
    // depende do papel de quem cria (convidado/logado/admin) e é validado no
    // controller; aqui só garantimos um teto absoluto saudável.
    public static final int DEFAULT_MAX_PLAYERS = 12;
    public static final int ABSOLUTE_MAX_PLAYERS = 48;

    public static GameConfig defaults() {
        return new GameConfig(RoomMode.INDIVIDUAL, ScoringMode.SPEED, AdvanceMode.HOST, 120, 10, null, GameStyle.NORMAL, DEFAULT_MAX_PLAYERS);
    }

    /** Preenche campos nulos com os defaults e aplica limites saudáveis. */
    public GameConfig withDefaults() {
        GameConfig d = defaults();
        int time = questionTimeSeconds != null ? questionTimeSeconds : d.questionTimeSeconds();
        int count = questionCount != null ? questionCount : d.questionCount();
        time = Math.max(5, Math.min(300, time));
        count = Math.max(1, Math.min(30, count));
        Integer maxPerTeam = maxPlayersPerTeam != null
                ? Math.max(2, Math.min(4, maxPlayersPerTeam))
                : null;
        int max = maxPlayers != null ? maxPlayers : DEFAULT_MAX_PLAYERS;
        max = Math.max(2, Math.min(ABSOLUTE_MAX_PLAYERS, max));
        return new GameConfig(
                roomMode != null ? roomMode : d.roomMode(),
                scoringMode != null ? scoringMode : d.scoringMode(),
                // Avanço automático foi descontinuado: partidas multiplayer são
                // sempre avançadas manualmente pelo líder.
                AdvanceMode.HOST,
                time,
                count,
                maxPerTeam,
                gameStyle != null ? gameStyle : d.gameStyle(),
                max
        );
    }
}
