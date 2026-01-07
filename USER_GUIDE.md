# LibSearch User Guide

**Version:** 1.0.0
**Last Updated:** December 2024

---

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Getting Started](#getting-started)
4. [GUI Mode](#gui-mode)
5. [CLI Mode](#cli-mode)
6. [Interactive REPL Mode](#interactive-repl-mode)
7. [Advanced Search Syntax](#advanced-search-syntax)
8. [Features Guide](#features-guide)
9. [Configuration](#configuration)
10. [Troubleshooting](#troubleshooting)
11. [FAQ](#faq)

---

## Introduction

LibSearch is an academic federated search application that helps you search across multiple academic databases simultaneously. Whether you're a student, researcher, or academic professional, LibSearch makes it easy to find, download, and manage academic resources.

### Key Features

- **Federated Search**: Query multiple sources (arXiv, PubMed, CrossRef, Semantic Scholar) at once
- **Advanced Query Syntax**: Use grep-like filters (author:, year:, type:) for precise searches
- **Offline Indexing**: Search your downloaded papers offline with full-text search
- **PDF Downloads**: Download papers with queue management and progress tracking
- **Bookmarks**: Save and organize your favorite papers
- **Citation Export**: Export citations in BibTeX, RIS, or EndNote format
- **Keyword Alerts**: Monitor for new papers matching your interests
- **Session Tracking**: Keep track of your research sessions
- **Dual Interface**: Use either the rich GUI or powerful CLI/REPL

---

## Installation

### Prerequisites

- **Java 21** or later (Amazon Corretto recommended)
- **Maven 3.8+** (for building from source)
- Internet connection for federated search

### Option 1: Pre-built JAR

1. Download the latest release JAR file
2. Place it in your preferred directory
3. Run: `java -jar libsearch-1.0.jar`

### Option 2: Build from Source

```bash
# Clone the repository
git clone <repository-url>
cd team-Se1-CSDC-HCW

# Build with Maven
mvn clean install

# The JAR will be in target/
java -jar target/team-Se1-CSDC-HCW-1.0-SNAPSHOT.jar
```

### First Run

On first run, LibSearch creates a configuration directory:
```
~/.libsearch/
├── config.yaml          # User configuration
├── data/
│   └── libsearch.db    # SQLite database
├── downloads/          # Downloaded PDFs
├── index/              # Lucene index
└── logs/
    └── libsearch.log   # Application logs
```

---

## Getting Started

### Launching LibSearch

**GUI Mode** (default):
```bash
java -jar libsearch.jar
```

**CLI Mode**:
```bash
java -jar libsearch.jar search "machine learning"
```

**Interactive REPL Mode**:
```bash
java -jar libsearch.jar --interactive
# or
java -jar libsearch.jar repl
```

---

## GUI Mode

### Main Window

The GUI features a clean, modern interface with:
- **Search Panel**: Enter queries and select sources
- **Results Table**: View search results with columns for title, authors, year, source
- **Menu Bar**: Access all features (File, Edit, View, Tools)
- **Status Bar**: Connection status and progress indicators

### Basic Search (GUI)

1. Enter your search query in the search field
2. (Optional) Select specific sources from the dropdown
3. Click **Search** or press Enter
4. Results appear in the table below

### Working with Results (GUI)

**View Details**:
- Double-click a result to see full details
- View abstract, authors, publication date, DOI

**Download PDF**:
- Right-click a result → "Download PDF"
- Check progress in the Downloads panel
- PDFs saved to `~/.libsearch/downloads/`

**Bookmark**:
- Right-click a result → "Add Bookmark"
- Add tags and notes for organization
- Access bookmarks via View menu

**Export Citation**:
- Select result(s) → File → Export Citations
- Choose format: BibTeX, RIS, EndNote, JSON, Markdown
- Save to file

### Offline Mode (GUI)

Toggle offline mode to search your local index:
1. View → Toggle Offline Mode
2. Search box now queries local Lucene index
3. Fast searches without internet connection

---

## CLI Mode

The CLI provides powerful command-line access to all LibSearch features.

### Basic Commands

```bash
# Show help
java -jar libsearch.jar --help

# Show version
java -jar libsearch.jar --version

# List all commands
java -jar libsearch.jar
```

### Search Command

**Basic Search**:
```bash
java -jar libsearch.jar search "machine learning"
```

**With Source Selection**:
```bash
java -jar libsearch.jar search "neural networks" -s arxiv,pubmed
```

**Offline Search**:
```bash
java -jar libsearch.jar search "deep learning" --offline
```

**Output Formats**:
```bash
# Table format (default)
java -jar libsearch.jar search "AI" -f table

# JSON format
java -jar libsearch.jar search "AI" -f json

# Simple text format
java -jar libsearch.jar search "AI" -f simple
```

**Limit Results**:
```bash
java -jar libsearch.jar search "quantum" -n 10
```

### Bookmark Commands

**List Bookmarks**:
```bash
java -jar libsearch.jar bookmark list
```

**Find Bookmarks**:
```bash
java -jar libsearch.jar bookmark find "neural"
```

**Delete Bookmark**:
```bash
java -jar libsearch.jar bookmark delete <bookmark-id>
```

### Index Commands

**Show Index Statistics**:
```bash
java -jar libsearch.jar index stats
```

**Optimize Index**:
```bash
java -jar libsearch.jar index optimize
```

**Clear Index**:
```bash
java -jar libsearch.jar index clear
```

### Network Commands

**Network Diagnostics**:
```bash
java -jar libsearch.jar network diag
```

**Configure Proxy**:
```bash
java -jar libsearch.jar network proxy set -h proxy.example.com -p 8080
java -jar libsearch.jar network proxy enable
java -jar libsearch.jar network proxy disable
```

**Test Connectivity**:
```bash
java -jar libsearch.jar network test arxiv.org
```

### Monitor Commands

**List Alerts**:
```bash
java -jar libsearch.jar monitor list
```

**Add Alert**:
```bash
java -jar libsearch.jar monitor add \
  -n "ML Papers" \
  -k "machine learning,deep learning" \
  -a Smith \
  --year-from 2020 \
  -t CONSOLE
```

**Check Alert**:
```bash
java -jar libsearch.jar monitor check <alert-id>
```

**Delete Alert**:
```bash
java -jar libsearch.jar monitor delete <alert-id>
```

### Session Commands

**List Recent Sessions**:
```bash
java -jar libsearch.jar session list
java -jar libsearch.jar session list -n 20
```

**View Activity Journal**:
```bash
# Recent activity (last 20 entries)
java -jar libsearch.jar session journal

# Last 50 entries
java -jar libsearch.jar session journal -n 50

# Activity for specific session
java -jar libsearch.jar session journal <session-id>
```

---

## Interactive REPL Mode

The REPL (Read-Eval-Print Loop) provides an interactive terminal session.

### Starting REPL

```bash
java -jar libsearch.jar --interactive
```

You'll see:
```
LibSearch REPL - Type 'exit' to quit
libsearch>
```

### REPL Commands

All CLI commands work in REPL mode without the `java -jar libsearch.jar` prefix:

```
libsearch> search "quantum computing" -s arxiv
libsearch> bookmark list
libsearch> index stats
libsearch> session list
libsearch> exit
```

### REPL Features

- **Command History**: Use ↑/↓ arrows to navigate history
- **Line Editing**: Standard terminal editing (Ctrl+A, Ctrl+E, etc.)
- **Quoted Arguments**: `search "neural networks" -s arxiv`
- **Persistent Session**: Your session state is maintained throughout

### Example REPL Session

```
libsearch> search "transformer architecture" -n 5 -s arxiv

Found 5 results:

╔════╦══════════════════════════════════════════════╗
║ #  ║ Title                                        ║
╠════╬══════════════════════════════════════════════╣
║ 1  ║ Attention Is All You Need                    ║
║ 2  ║ BERT: Pre-training of Deep Transformers      ║
║ 3  ║ GPT-3: Language Models are Few-Shot Learners ║
║ 4  ║ Vision Transformer (ViT)                     ║
║ 5  ║ Transformer-XL                               ║
╚════╩══════════════════════════════════════════════╝

libsearch> bookmark list
[Shows bookmarks...]

libsearch> exit
Goodbye!
```

---

## Advanced Search Syntax

LibSearch supports powerful grep-like query syntax for precise searches.

### Basic Operators

**AND** (implicit or explicit):
```
machine learning        # Both words (implicit AND)
machine AND learning    # Explicit AND
```

**OR**:
```
"neural networks" OR "deep learning"
```

**NOT** (exclusion):
```
machine learning NOT tutorial
machine learning -tutorial    # Alternative syntax
```

**Required** (+):
```
+machine learning    # "machine" is required
```

**Phrases**:
```
"convolutional neural networks"
```

### Field Filters

**Author Filter**:
```
machine learning author:Hinton
neural networks author:Bengio author:LeCun    # Multiple authors
```

**Year Filters**:
```
# Exact year
AI year:2024

# After year
machine learning year:>2020

# Before year
neural networks year:<2023

# Year range
deep learning year:2020..2024
```

**Document Type**:
```
machine learning type:article
neural networks type:thesis
AI type:book
```

**Site/Source**:
```
quantum computing site:arxiv.org
medical research site:pubmed.ncbi.nlm.nih.gov
```

**File Type**:
```
machine learning filetype:pdf
```

### Complex Query Examples

**Example 1: Recent AI Papers by Specific Author**
```
"artificial intelligence" author:Russell year:>2020 type:article
```

**Example 2: Deep Learning Papers Excluding Tutorials**
```
"deep learning" -tutorial -introduction year:2022..2024
```

**Example 3: Medical ML Papers**
```
(machine learning OR "deep learning") AND (medical OR healthcare) year:>2021
```

**Example 4: Computer Vision Papers from arXiv**
```
"computer vision" site:arxiv.org year:2023 filetype:pdf
```

---

## Features Guide

### 1. PDF Downloads

**Single Download**:
- GUI: Right-click result → Download PDF
- CLI: Results include download URLs

**Bulk Download**:
- GUI: Select multiple results → File → Download Selected
- Downloads are queued and processed concurrently (default: 3 at a time)

**Download Progress**:
- GUI: View → Downloads panel shows progress bars
- Files saved to `~/.libsearch/downloads/`

**Download Settings**:
Edit `~/.libsearch/config.yaml`:
```yaml
download:
  output_dir: ~/Documents/Papers  # Custom download location
  max_concurrent: 5                # Increase concurrent downloads
  retry_attempts: 3                # Retry failed downloads
```

### 2. Bookmarks

**Adding Bookmarks**:
- GUI: Right-click result → Add Bookmark
- Add tags (comma-separated) and notes

**Organizing Bookmarks**:
- Filter by tags
- Search bookmark titles and notes
- Sort by date added

**Exporting Bookmarks**:
```bash
# CLI: Export to JSON
java -jar libsearch.jar bookmark export bookmarks.json

# Export to Markdown
java -jar libsearch.jar bookmark export bookmarks.md
```

### 3. Citation Export

**Supported Formats**:
- **BibTeX**: For LaTeX documents
- **RIS**: For EndNote, Zotero, Mendeley
- **EndNote**: Direct EndNote XML format
- **JSON**: Machine-readable format
- **Markdown**: Human-readable format

**Exporting**:
```bash
# GUI: Select results → File → Export Citations → Choose format

# CLI: Include in search results (JSON format)
java -jar libsearch.jar search "AI" -f json > results.json
```

**BibTeX Example**:
```bibtex
@article{vaswani2017attention,
  title={Attention Is All You Need},
  author={Vaswani, Ashish and Shazeer, Noam and Parmar, Niki},
  journal={arXiv preprint arXiv:1706.03762},
  year={2017}
}
```

### 4. Keyword Alerts

**Creating Alerts**:
```bash
java -jar libsearch.jar monitor add \
  -n "Quantum Computing Papers" \
  -k "quantum computing,quantum algorithms" \
  -a "Nielsen" \
  --year-from 2024 \
  -s arxiv,crossref \
  -t CONSOLE
```

**Alert Notifications**:
- **CONSOLE**: Print to terminal
- **LOG**: Write to log file (`~/.libsearch/logs/libsearch.log`)
- **EMAIL**: Send email (requires SMTP configuration)

**Checking Alerts Manually**:
```bash
# Check specific alert
java -jar libsearch.jar monitor check <alert-id>

# Check all alerts
java -jar libsearch.jar monitor check-all
```

**Automated Checking**:
Use cron or Task Scheduler to run checks periodically:
```bash
# Cron example (check alerts daily at 9 AM)
0 9 * * * java -jar /path/to/libsearch.jar monitor check-all
```

### 5. Offline Search (Local Index)

**How It Works**:
- Search results are automatically indexed using Apache Lucene
- Index includes full text (title, abstract, authors)
- Supports field-specific searches and year ranges

**Using Offline Search**:
```bash
# GUI: Toggle offline mode in View menu
# CLI:
java -jar libsearch.jar search "machine learning" --offline
```

**Index Management**:
```bash
# View statistics
java -jar libsearch.jar index stats

# Output:
# Total documents: 1,234
# Index size: 45.2 MB
# Last updated: 2024-12-28 14:30:22

# Optimize index (merge segments)
java -jar libsearch.jar index optimize

# Clear index (delete all)
java -jar libsearch.jar index clear
```

### 6. Network Features

**Proxy Configuration**:
```bash
# Set HTTP proxy
java -jar libsearch.jar network proxy set \
  -h proxy.company.com \
  -p 8080 \
  -t HTTP

# Set SOCKS proxy
java -jar libsearch.jar network proxy set \
  -h socks.company.com \
  -p 1080 \
  -t SOCKS

# Enable proxy
java -jar libsearch.jar network proxy enable

# Show current proxy settings
java -jar libsearch.jar network proxy show

# Disable proxy
java -jar libsearch.jar network proxy disable
```

**Network Diagnostics**:
```bash
java -jar libsearch.jar network diag

# Output:
# Network Diagnostics
# ──────────────────────────────────────
# Local Hostname: research-laptop
# Internet: Connected
#
# Source Connectivity:
#   ✓ arxiv.org - Reachable (124ms)
#   ✓ pubmed.ncbi.nlm.nih.gov - Reachable (156ms)
#   ✗ crossref.org - Timeout
#   ✓ api.semanticscholar.org - Reachable (201ms)
```

**Testing Specific Hosts**:
```bash
java -jar libsearch.jar network test arxiv.org
```

### 7. Session Tracking

**Automatic Session Management**:
- Sessions start automatically when you launch LibSearch
- Track searches, downloads, bookmarks per session
- Calculate session duration and statistics

**Viewing Sessions**:
```bash
# List recent sessions
java -jar libsearch.jar session list -n 10

# Output:
# Recent Sessions (10):
# ────────────────────────────────────────────────────
# 1. Session: abc123...
#    Started: 2024-12-28 09:15:32
#    Ended: 2024-12-28 11:45:18
#    Duration: 2h 29m 46s
#    Searches: 12
#
# 2. Session: def456...
#    Started: 2024-12-27 14:20:10
#    Ended: 2024-12-27 16:05:42
#    Duration: 1h 45m 32s
#    Searches: 8
```

**Activity Journal**:
```bash
# View recent activity
java -jar libsearch.jar session journal -n 20

# View activity for specific session
java -jar libsearch.jar session journal abc123...

# Output:
# Journal for session: abc123...
# ────────────────────────────────────────────────────
# [2024-12-28 09:15:32] SESSION: Session started
# [2024-12-28 09:16:45] SEARCH: Query: "machine learning"
# [2024-12-28 09:18:20] DOWNLOAD: Downloaded paper: attention.pdf
# [2024-12-28 09:22:10] BOOKMARK: Added bookmark: BERT Paper
# [2024-12-28 09:30:55] EXPORT: Exported 5 citations to BibTeX
```

---

## Configuration

### Configuration File Location

User configuration: `~/.libsearch/config.yaml`

### Configuration Options

```yaml
# Source Configuration
sources:
  arxiv:
    enabled: true
    max_results: 50
    rate_limit: 3  # requests per second

  pubmed:
    enabled: true
    api_key: ${PUBMED_API_KEY}  # From environment variable
    max_results: 50

  crossref:
    enabled: true
    max_results: 50

  semantic_scholar:
    enabled: true
    max_results: 50

# Search Settings
search:
  max_concurrent_sources: 5  # Parallel source queries
  timeout_seconds: 30        # Per-source timeout
  auto_index: true           # Auto-index results

# Download Settings
download:
  output_dir: ~/.libsearch/downloads
  max_concurrent: 3
  retry_attempts: 3
  timeout_seconds: 60

# Database Settings
database:
  path: ~/.libsearch/data/libsearch.db

# Index Settings
index:
  directory: ~/.libsearch/index
  commit_interval: 100  # Documents per commit

# Logging Settings
logging:
  level: INFO  # DEBUG, INFO, WARN, ERROR
  file: ~/.libsearch/logs/libsearch.log
  max_file_size: 10MB
  max_files: 5

# UI Settings (GUI only)
ui:
  theme: dark  # dark or light
  results_per_page: 50
```

### Environment Variables

**API Keys**:
```bash
export PUBMED_API_KEY="your-api-key-here"
export SEMANTIC_SCHOLAR_API_KEY="your-api-key-here"
```

**Override Config Path**:
```bash
export LIBSEARCH_CONFIG_DIR="~/custom/config/path"
```

---

## Troubleshooting

### Common Issues

#### 1. "No results found" for all sources

**Possible Causes**:
- No internet connection
- Proxy/firewall blocking access
- API rate limits exceeded

**Solutions**:
```bash
# Test connectivity
java -jar libsearch.jar network diag

# Check proxy settings
java -jar libsearch.jar network proxy show

# Try offline search
java -jar libsearch.jar search "query" --offline
```

#### 2. Downloads failing

**Possible Causes**:
- Paywall/access restrictions
- Invalid URL
- Network timeout

**Solutions**:
- Check if PDF is open access
- Try downloading from different source
- Increase timeout in config:
  ```yaml
  download:
    timeout_seconds: 120
  ```

#### 3. JavaFX errors on Linux

**Error**: "Could not find or load main class"

**Solution**: Install JavaFX runtime
```bash
# Ubuntu/Debian
sudo apt-get install openjfx

# Or use Maven to run:
mvn javafx:run
```

#### 4. Index corruption

**Error**: "CorruptIndexException"

**Solution**: Rebuild index
```bash
java -jar libsearch.jar index clear
# Then perform searches to rebuild
```

#### 5. Database locked

**Error**: "database is locked"

**Solution**:
- Ensure only one LibSearch instance is running
- Delete lock file: `rm ~/.libsearch/data/libsearch.db-journal`

### Logging

**Enable Debug Logging**:

Edit `~/.libsearch/config.yaml`:
```yaml
logging:
  level: DEBUG
```

**View Logs**:
```bash
tail -f ~/.libsearch/logs/libsearch.log
```

### Getting Help

**Built-in Help**:
```bash
java -jar libsearch.jar --help
java -jar libsearch.jar search --help
java -jar libsearch.jar bookmark --help
```

**Version Information**:
```bash
java -jar libsearch.jar --version
```

---

## FAQ

### General Questions

**Q: Is LibSearch free?**
A: Yes, LibSearch is open-source software for academic use.

**Q: Do I need API keys?**
A: Most sources work without API keys. PubMed API key is optional but increases rate limits.

**Q: Can I use LibSearch offline?**
A: Yes, use offline mode to search your local index of previously downloaded papers.

**Q: What sources are supported?**
A: arXiv, PubMed, CrossRef, Semantic Scholar, Google Scholar (limited), DuckDuckGo, Bing, Brave, Wikipedia, GitHub, StackOverflow, Reddit.

### Search Questions

**Q: How do I search for an exact phrase?**
A: Use quotes: `"neural networks"`

**Q: Can I search multiple authors?**
A: Yes: `machine learning author:Hinton author:Bengio`

**Q: How do I exclude words?**
A: Use NOT or minus: `AI NOT tutorial` or `AI -tutorial`

**Q: What date formats are supported?**
A: Use year filters: `year:2024`, `year:>2020`, `year:2020..2024`

### Download Questions

**Q: Where are PDFs saved?**
A: Default: `~/.libsearch/downloads/` (configurable)

**Q: Can I download multiple papers at once?**
A: Yes, LibSearch queues downloads and processes them concurrently.

**Q: Why can't I download some papers?**
A: Papers behind paywalls require institutional access. Use VPN or proxy if available.

### Technical Questions

**Q: What Java version is required?**
A: Java 21 or later (Amazon Corretto recommended)

**Q: Can I run LibSearch on a server?**
A: Yes, use CLI mode for server deployment:
```bash
java -jar libsearch.jar search "query" -f json
```

**Q: How much disk space does the index use?**
A: Approximately 30-50 MB per 1,000 indexed documents.

**Q: Can I export my bookmarks?**
A: Yes, to JSON or Markdown format.

### Advanced Questions

**Q: How do I schedule automated searches?**
A: Use keyword alerts with cron/Task Scheduler:
```bash
0 9 * * * java -jar libsearch.jar monitor check-all
```

**Q: Can I customize the sources?**
A: Yes, edit `~/.libsearch/config.yaml` to enable/disable sources.

**Q: How do I use a proxy?**
A: See [Network Features](#6-network-features) section.

**Q: Can I integrate LibSearch with other tools?**
A: Yes, use CLI mode with JSON output format for scripting.

---

## Quick Reference Card

### Essential Commands

```bash
# GUI Mode
java -jar libsearch.jar

# CLI Search
java -jar libsearch.jar search "<query>" [-s sources] [-f format]

# REPL Mode
java -jar libsearch.jar --interactive

# Bookmarks
java -jar libsearch.jar bookmark list|find|delete

# Index
java -jar libsearch.jar index stats|optimize|clear

# Network
java -jar libsearch.jar network diag|proxy|test

# Monitors
java -jar libsearch.jar monitor list|add|check|delete

# Sessions
java -jar libsearch.jar session list|journal
```

### Query Syntax Cheatsheet

```
author:Smith              # Filter by author
year:2024                 # Exact year
year:>2020                # After 2020
year:2020..2024           # Year range
type:article              # Document type
site:arxiv.org            # Specific site
filetype:pdf              # File type
"exact phrase"            # Phrase search
term1 AND term2           # Both required
term1 OR term2            # Either required
term1 NOT term2           # Exclude term2
+term1 term2              # term1 required, term2 optional
```

---

**Developers**: qwitch13 (nebulai13) & zahieddo
**Documentation Version**: 1.0
**Last Updated**: December 2024

For technical details, see [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)
