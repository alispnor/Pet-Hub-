-- ============================================================
-- V0 — Extensões PostgreSQL necessárias
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm; -- busca por nome de produto via trigram

-- Reservado para Fase 10 (RAG/IA):
-- CREATE EXTENSION IF NOT EXISTS vector;
