ALTER TABLE produtos
    ADD COLUMN video_url VARCHAR(500);

COMMENT ON COLUMN produtos.video_url IS 'URL externa de vídeo (YouTube/Vimeo) — validada por @ValidVideoEmbedUrl no DTO.';
