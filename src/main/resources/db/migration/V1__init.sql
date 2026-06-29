-- Enable pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- purposes table stores business purposes and vector embeddings
CREATE TABLE IF NOT EXISTS purposes (
  id serial PRIMARY KEY,
  purpose_text text NOT NULL,
  embedding vector(1536)
);

-- Insert sample purposes (embeddings will be populated on startup)
INSERT INTO purposes (purpose_text) VALUES
('Provide cloud-native payments infrastructure'),
('Develop AI-powered healthcare diagnostics'),
('Offer e-commerce platform for small retailers'),
('Create mobile games for casual players'),
('Consulting for digital transformation');
