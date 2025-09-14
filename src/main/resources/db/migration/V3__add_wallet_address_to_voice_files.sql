-- Add walletAddress field to voice_files table for better performance
-- This allows direct wallet-based queries without joining the users table

-- Add the wallet_address column
ALTER TABLE voice_files 
ADD COLUMN wallet_address VARCHAR(42);

-- Populate existing records with wallet addresses from users table
UPDATE voice_files 
SET wallet_address = (
    SELECT u.wallet_address 
    FROM users u 
    WHERE u.id = voice_files.user_id
);

-- Make the column NOT NULL after populating
ALTER TABLE voice_files 
ALTER COLUMN wallet_address SET NOT NULL;

-- Add index for performance on wallet-based queries
CREATE INDEX idx_voice_file_wallet_address ON voice_files(wallet_address);

-- Add composite index for common query patterns
CREATE INDEX idx_voice_file_wallet_status ON voice_files(wallet_address, status);
CREATE INDEX idx_voice_file_wallet_uploaded ON voice_files(wallet_address, uploaded_at DESC);