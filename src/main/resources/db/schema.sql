-- LibSearch Database Schema
-- SQLite database for search history, bookmarks, alerts, and session management

-- Session tracking
CREATE TABLE IF NOT EXISTS sessions (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT -- JSON
);

-- Search history
CREATE TABLE IF NOT EXISTS search_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    query TEXT NOT NULL,
    parsed_query TEXT, -- JSON representation
    sources TEXT, -- JSON array of source types
    result_count INTEGER DEFAULT 0,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
CREATE INDEX IF NOT EXISTS idx_search_history_session ON search_history(session_id);
CREATE INDEX IF NOT EXISTS idx_search_history_time ON search_history(executed_at);

-- Search results (cached)
CREATE TABLE IF NOT EXISTS search_results (
    id TEXT PRIMARY KEY, -- MD5 hash of URL or DOI
    search_history_id INTEGER,
    title TEXT NOT NULL,
    authors TEXT, -- JSON array or comma-separated
    url TEXT NOT NULL,
    snippet TEXT,
    abstract TEXT,
    source_type TEXT NOT NULL, -- ARXIV, PUBMED, etc.
    access_level TEXT DEFAULT 'UNKNOWN', -- OPEN_ACCESS, LICENSED, RESTRICTED
    publication_date TEXT,
    doi TEXT,
    arxiv_id TEXT,
    pmid TEXT,
    keywords TEXT, -- JSON array
    journal TEXT,
    venue TEXT,
    citations INTEGER DEFAULT 0,
    pdf_url TEXT,
    relevance REAL DEFAULT 0.0,
    indexed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT, -- JSON for source-specific data
    FOREIGN KEY (search_history_id) REFERENCES search_history(id)
);
CREATE INDEX IF NOT EXISTS idx_results_source ON search_results(source_type);
CREATE INDEX IF NOT EXISTS idx_results_doi ON search_results(doi);
CREATE INDEX IF NOT EXISTS idx_results_arxiv ON search_results(arxiv_id);
CREATE INDEX IF NOT EXISTS idx_results_pmid ON search_results(pmid);

-- Bookmarks
CREATE TABLE IF NOT EXISTS bookmarks (
                                         id TEXT PRIMARY KEY,
                                         result_id TEXT,
                                         title TEXT NOT NULL,
                                         url TEXT NOT NULL,
                                         notes TEXT,
                                         tags TEXT,
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Alerts (keyword monitoring)
CREATE TABLE IF NOT EXISTS alerts (
    id TEXT PRIMARY KEY,
    keyword TEXT NOT NULL,
    category TEXT,
    enabled BOOLEAN DEFAULT 1,
    notification_method TEXT DEFAULT 'CONSOLE', -- CONSOLE, EMAIL, LOG
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_triggered TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_alerts_keyword ON alerts(keyword);

-- Alert matches
CREATE TABLE IF NOT EXISTS alert_matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    alert_id TEXT NOT NULL,
    result_id TEXT NOT NULL,
    context TEXT, -- Text snippet with keyword highlighted
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_id) REFERENCES alerts(id),
    FOREIGN KEY (result_id) REFERENCES search_results(id)
);
CREATE INDEX IF NOT EXISTS idx_matches_alert ON alert_matches(alert_id);
CREATE INDEX IF NOT EXISTS idx_matches_time ON alert_matches(triggered_at);

-- Download queue
CREATE TABLE IF NOT EXISTS downloads (
    id TEXT PRIMARY KEY,
    result_id TEXT,
    url TEXT NOT NULL,
    destination_path TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED
    progress REAL DEFAULT 0.0,
    file_size INTEGER,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    FOREIGN KEY (result_id) REFERENCES search_results(id)
);
CREATE INDEX IF NOT EXISTS idx_downloads_status ON downloads(status);

-- Configuration settings
CREATE TABLE IF NOT EXISTS config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'STRING', -- STRING, INTEGER, BOOLEAN, JSON
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Journal entries (audit log)
CREATE TABLE IF NOT EXISTS journal (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id TEXT NOT NULL,
    entry_type TEXT NOT NULL, -- SEARCH_START, SEARCH_RESULT, DOWNLOAD, ERROR, etc.
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data TEXT NOT NULL, -- JSON
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
CREATE INDEX IF NOT EXISTS idx_journal_session ON journal(session_id);
CREATE INDEX IF NOT EXISTS idx_journal_type ON journal(entry_type);
CREATE INDEX IF NOT EXISTS idx_journal_time ON journal(timestamp);
