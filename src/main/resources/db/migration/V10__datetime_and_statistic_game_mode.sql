-- Migração para exibir data + hora em respostas e estatísticas, e para
-- registrar o modo de jogo em cada estatística por conclusão.
--
-- Preserva os dados já cadastrados: valores existentes de tipo `date`
-- viram `timestamp` na meia-noite (00:00:00).

-- tb_response.date_time: date -> timestamp
ALTER TABLE tb_response
    ALTER COLUMN date_time TYPE timestamp USING date_time::timestamp;

-- tb_statistic.date: date -> timestamp
ALTER TABLE tb_statistic
    ALTER COLUMN date TYPE timestamp USING date::timestamp;

-- Novo campo game_mode em tb_statistic (default SINGLE_PLAYER pros dados
-- existentes, que só eram salvos no modo um jogador).
ALTER TABLE tb_statistic
    ADD COLUMN IF NOT EXISTS game_mode varchar(30);

UPDATE tb_statistic SET game_mode = 'SINGLE_PLAYER' WHERE game_mode IS NULL;

ALTER TABLE tb_statistic
    ALTER COLUMN game_mode SET NOT NULL;

ALTER TABLE tb_statistic
    ADD CONSTRAINT tb_statistic_game_mode_check
    CHECK (game_mode IN ('SINGLE_PLAYER', 'MULTIPLAYER'));

CREATE INDEX IF NOT EXISTS idx_statistic_game_mode ON tb_statistic (game_mode);
