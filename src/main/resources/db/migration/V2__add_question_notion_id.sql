ALTER TABLE questions
    ADD COLUMN notion_id VARCHAR(30) NULL COMMENT '노션 원본 문제 ID (DAMIT-1 등)' AFTER id,
    ADD KEY idx_questions_notion_id_active (notion_id, is_active);
