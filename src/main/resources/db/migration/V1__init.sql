CREATE TABLE voice_file (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255),
    content_type VARCHAR(128),
    size BIGINT,
    duration FLOAT,
    uploaded_at TIMESTAMP,
    status VARCHAR(32),
    job_id VARCHAR(128),
    error_msg VARCHAR(2000)
    -- 필요한 제약조건, 외래키 등 추가
);
