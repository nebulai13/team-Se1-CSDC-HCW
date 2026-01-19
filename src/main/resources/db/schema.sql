-- ============================================================================
-- libsearch database schema
-- sqlite database for search history, bookmarks, alerts, and session management
-- ============================================================================
-- sqlite = lightweight embedded db, stores data in single file
-- runs in-process, no server needed
-- file location: ~/.libsearch/data/libsearch.db
-- ============================================================================

-- ----------------------------------------------------------------------------
-- sessions table: tracks research sessions
-- a session groups related searches for a research project
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id TEXT PRIMARY KEY,                                    -- uuid string
    name TEXT NOT NULL,                                     -- human-readable name
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- when session started
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- last activity
    metadata TEXT -- JSON                                   -- extra data (json blob)
);

-- ----------------------------------------------------------------------------
-- search_history table: logs all queries
-- stores what user searched for + result count
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS search_history (
    id TEXT PRIMARY KEY,                                    -- uuid for each search
    session_id TEXT NOT NULL,                               -- which session this belongs to
    query_text TEXT NOT NULL,                               -- raw user query string
    parsed_query TEXT, -- JSON representation               -- structured query as json
    sources TEXT, -- JSON array of source types             -- which apis were searched
    result_count INTEGER DEFAULT 0,                         -- how many results returned
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- when search ran
    FOREIGN KEY (session_id) REFERENCES sessions(id)        -- fk to sessions table
);
-- indexes speed up queries on these columns
CREATE INDEX IF NOT EXISTS idx_search_history_session ON search_history(session_id);
CREATE INDEX IF NOT EXISTS idx_search_history_time ON search_history(timestamp);

-- ----------------------------------------------------------------------------
-- search_results table: cached paper metadata
-- stores full paper details from api responses
-- enables offline access to previously searched papers
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS search_results (
    id TEXT PRIMARY KEY,                                    -- uuid
    session_id TEXT,                                        -- which session found this
    title TEXT NOT NULL,                                    -- paper title
    authors TEXT, -- JSON array or comma-separated          -- author names
    url TEXT NOT NULL,                                      -- link to paper
    snippet TEXT,                                           -- short preview text
    abstract_text TEXT,                                     -- full abstract
    source TEXT NOT NULL, -- ARXIV, PUBMED, etc.            -- which api it came from
    access_level TEXT DEFAULT 'UNKNOWN', -- OPEN_ACCESS, LICENSED, RESTRICTED -- availability
    publication_date TEXT,                                  -- when published (iso format)
    doi TEXT,                                               -- digital object identifier
    arxiv_id TEXT,                                          -- arxiv preprint id
    pmid TEXT,                                              -- pubmed id
    keywords TEXT, -- JSON array                            -- subject keywords
    journal TEXT,                                           -- journal name
    venue TEXT,                                             -- conference venue
    citation_count INTEGER DEFAULT 0,                       -- times cited
    pdf_url TEXT,                                           -- direct pdf link
    relevance REAL DEFAULT 0.0,                             -- search score 0-1
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- when cached
    metadata TEXT, -- JSON for source-specific data         -- extra source-specific info
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
-- indexes on common query fields for fast lookup
CREATE INDEX IF NOT EXISTS idx_results_source ON search_results(source);
CREATE INDEX IF NOT EXISTS idx_results_doi ON search_results(doi);
CREATE INDEX IF NOT EXISTS idx_results_arxiv ON search_results(arxiv_id);
CREATE INDEX IF NOT EXISTS idx_results_pmid ON search_results(pmid);

-- ----------------------------------------------------------------------------
-- bookmarks table: user-saved papers
-- allows users to save papers for later reference
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookmarks (
                                         id TEXT PRIMARY KEY,            -- bookmark uuid
                                         result_id TEXT,                 -- fk to search_results
                                         title TEXT NOT NULL,            -- paper title (cached)
                                         url TEXT NOT NULL,              -- paper url (cached)
                                         notes TEXT,                     -- user notes
                                         tags TEXT,                      -- comma-sep tags for categorization
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- alerts table: keyword monitoring config
-- like google scholar alerts: notifies when new papers match
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alerts (
    id TEXT PRIMARY KEY,                                    -- alert uuid
    keyword TEXT NOT NULL,                                  -- keyword(s) to monitor
    category TEXT,                                          -- optional category filter
    enabled BOOLEAN DEFAULT 1,                              -- 1=active, 0=paused
    notification_method TEXT DEFAULT 'CONSOLE', -- CONSOLE, EMAIL, LOG -- how to notify
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_triggered TIMESTAMP                                -- when last match found
);
CREATE INDEX IF NOT EXISTS idx_alerts_keyword ON alerts(keyword);

-- ----------------------------------------------------------------------------
-- alert_matches table: records triggered alerts
-- tracks which papers matched which alerts
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alert_matches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,                   -- auto-increment int id
    alert_id TEXT NOT NULL,                                 -- which alert triggered
    result_id TEXT NOT NULL,                                -- which paper matched
    context TEXT, -- Text snippet with keyword highlighted  -- context around match
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alert_id) REFERENCES alerts(id),
    FOREIGN KEY (result_id) REFERENCES search_results(id)
);
CREATE INDEX IF NOT EXISTS idx_matches_alert ON alert_matches(alert_id);
CREATE INDEX IF NOT EXISTS idx_matches_time ON alert_matches(triggered_at);

-- ----------------------------------------------------------------------------
-- downloads table: pdf download queue
-- manages async downloads with progress tracking
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS downloads (
    id TEXT PRIMARY KEY,                                    -- download uuid
    result_id TEXT,                                         -- which paper being downloaded
    url TEXT NOT NULL,                                      -- pdf url
    destination_path TEXT NOT NULL,                         -- local file path
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, FAILED
    progress REAL DEFAULT 0.0,                              -- 0.0-1.0 completion
    file_size INTEGER,                                      -- bytes
    started_at TIMESTAMP,                                   -- when download began
    completed_at TIMESTAMP,                                 -- when finished
    error_message TEXT,                                     -- error if failed
    FOREIGN KEY (result_id) REFERENCES search_results(id)
);
CREATE INDEX IF NOT EXISTS idx_downloads_status ON downloads(status);

-- ----------------------------------------------------------------------------
-- config table: app settings (key-value store)
-- stores user preferences and app config
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS config (
    key TEXT PRIMARY KEY,                                   -- setting name
    value TEXT NOT NULL,                                    -- setting value (as string)
    type TEXT NOT NULL DEFAULT 'STRING', -- STRING, INTEGER, BOOLEAN, JSON -- value type
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- journal table: audit log of all activities
-- records all user actions for debugging & analytics
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journal (
    id INTEGER PRIMARY KEY AUTOINCREMENT,                   -- auto-increment id
    session_id TEXT NOT NULL,                               -- which session
    entry_type TEXT NOT NULL, -- SEARCH_START, SEARCH_RESULT, DOWNLOAD, ERROR, etc.
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- when action occurred
    data TEXT NOT NULL, -- JSON                             -- action details as json
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);
CREATE INDEX IF NOT EXISTS idx_journal_session ON journal(session_id);
CREATE INDEX IF NOT EXISTS idx_journal_type ON journal(entry_type);
CREATE INDEX IF NOT EXISTS idx_journal_time ON journal(timestamp);
