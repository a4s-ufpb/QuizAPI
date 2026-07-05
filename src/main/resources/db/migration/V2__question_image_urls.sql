-- Migração das imagens de questão de base64 (armazenado no Postgres) para
-- URLs do MinIO. Imagens já cadastradas em base64 não são migradas
-- automaticamente (o usuário reedita a questão e faz o upload de novo, que
-- já sobe pro MinIO) — perda assumida e combinada previamente.

ALTER TABLE tb_question
    ADD COLUMN image_one_url varchar(255),
    ADD COLUMN image_two_url varchar(255);

ALTER TABLE tb_question
    DROP COLUMN image_base64one,
    DROP COLUMN image_base64two;
