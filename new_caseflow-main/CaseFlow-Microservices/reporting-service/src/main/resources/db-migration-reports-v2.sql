-- ════════════════════════════════════════════════════════════════════════════
-- REPORTING SERVICE — SCHEMA MIGRATION TO v2
--
-- Run this ONCE against caseflow_db before starting the new reporting-service.
-- Hibernate `ddl-auto: update` will NOT change column types automatically, so
-- this migration is required.
--
-- Changes:
--   1. requested_by  : BIGINT → VARCHAR(255)   (now stores user email/id from JWT)
--   2. date_from     : new column (nullable)   (PERIOD scope filter)
--   3. date_to       : new column (nullable)   (PERIOD scope filter)
--
-- If you don't have any production reports yet, the simplest path is:
--   DROP TABLE reports;
-- and let Hibernate recreate it on next startup.
-- ════════════════════════════════════════════════════════════════════════════

USE caseflow_db;

-- Option A — preserve existing rows (default)
ALTER TABLE reports
    MODIFY COLUMN requested_by VARCHAR(255) NOT NULL;

ALTER TABLE reports
    ADD COLUMN date_from DATE NULL,
    ADD COLUMN date_to   DATE NULL;

-- Option B — drop & recreate (USE ONLY IF YOU DON'T NEED OLD REPORTS)
-- DROP TABLE reports;
