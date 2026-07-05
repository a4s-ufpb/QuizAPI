package br.ufpb.dcx.apps4society.quizapi.game.model;

/**
 * Poderes que o líder pode ativar (modo Diversão) pra valer na próxima
 * questão. Escolhido na tela de resultado da questão anterior (BETWEEN) e
 * consumido (volta a nulo) assim que a próxima questão começa.
 */
public enum QuestionPower {
    SCORE_1_5X,
    SCORE_2_0X,
    SCORE_2_5X,
    HIDE_WRONG_ALTERNATIVE,
    HIDE_ALTERNATIVE_TEXTS,
    BLINK_SCREEN,
    SHAKE_SCREEN,
    STEAL_POINTS;

    public double scoreMultiplier() {
        return switch (this) {
            case SCORE_1_5X -> 1.5;
            case SCORE_2_0X -> 2.0;
            case SCORE_2_5X -> 2.5;
            default -> 1.0;
        };
    }
}
