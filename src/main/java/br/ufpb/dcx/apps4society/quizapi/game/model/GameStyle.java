package br.ufpb.dcx.apps4society.quizapi.game.model;

/**
 * Estilo da partida:
 * NORMAL — regras convencionais.
 * FUN — poderes ativáveis pelo líder entre questões.
 * SURVIVAL — quem erra (ou não responde) é eliminado e vira espectador.
 * LIGHTNING — tempo de resposta diminui a cada questão.
 */
public enum GameStyle {
    NORMAL,
    FUN,
    SURVIVAL,
    LIGHTNING
}
