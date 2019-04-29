CREATE TABLE IF NOT EXISTS files (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), name VARCHAR(255) NOT NULL, dir VARCHAR(50) UNIQUE NOT NULL, format VARCHAR(10) NOT NULL, language VARCHAR(5) NOT NULL, description TEXT, status VARCHAR(20) NOT NULL CHECK (status IN ('transcript_requested', 'transcript_pending', 'transcript_ready', 'transcript_failed', 'analysis_requested', 'analysis_pending', 'analysis_ready', 'analysis_failed')) DEFAULT 'transcript_requested', created TIMESTAMPTZ DEFAULT now())

CREATE TABLE IF NOT EXISTS transcripts (id SERIAL PRIMARY KEY, content TEXT NOT NULL, file_id UUID)

CREATE INDEX ON transcripts (file_id)

ALTER TABLE transcripts DROP CONSTRAINT IF EXISTS file
ALTER TABLE transcripts ADD CONSTRAINT file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE

CREATE TABLE IF NOT EXISTS analyses (id SERIAL PRIMARY KEY, content TEXT NOT NULL, file_id UUID)

CREATE INDEX ON analyses (file_id)

ALTER TABLE analyses DROP CONSTRAINT IF EXISTS file
ALTER TABLE analyses ADD CONSTRAINT file FOREIGN KEY (file_id) REFERENCES files (id) ON DELETE CASCADE
