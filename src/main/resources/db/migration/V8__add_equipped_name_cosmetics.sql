-- Adiciona colunas de cosméticos do nome na tabela tb_user
ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS equipped_font VARCHAR(255),
    ADD COLUMN IF NOT EXISTS equipped_name_style VARCHAR(255),
    ADD COLUMN IF NOT EXISTS equipped_name_effect VARCHAR(255);