-- Cosméticos equipados pelo usuário (títulos, molduras e banners da loja).
-- Puramente estético; guarda apenas o código do item equipado por slot.
ALTER TABLE tb_user
    ADD COLUMN equipped_title  varchar(50),
    ADD COLUMN equipped_frame  varchar(50),
    ADD COLUMN equipped_banner varchar(50);
