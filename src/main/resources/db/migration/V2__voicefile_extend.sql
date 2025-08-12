DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='voice_file' AND column_name='original_filename'
    ) THEN
        ALTER TABLE voice_file ADD COLUMN original_filename varchar(255);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='voice_file' AND column_name='content_type'
    ) THEN
        ALTER TABLE voice_file ADD COLUMN content_type varchar(128);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='voice_file' AND column_name='size'
    ) THEN
        ALTER TABLE voice_file ADD COLUMN size bigint;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='voice_file' AND column_name='status'
    ) THEN
        ALTER TABLE voice_file ADD COLUMN status varchar(32);
        UPDATE voice_file SET status = 'UPLOADED' WHERE status IS NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='voice_file' AND column_name='job_id'
    ) THEN
        ALTER TABLE voice_file ADD COLUMN job_id varchar(128);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name='voice_file' AND column_name='error_msg'
    ) THEN
        ALTER TABLE voice_file ADD COLUMN error_msg varchar(2000);
    END IF;
END$$;
