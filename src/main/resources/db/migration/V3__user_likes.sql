-- Contador de likes que um usuário recebe de outros jogadores (multiplayer).
ALTER TABLE tb_user
    ADD COLUMN likes integer NOT NULL DEFAULT 0;
