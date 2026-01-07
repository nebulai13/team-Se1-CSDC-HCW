# LibSearch Quick Start Guide

Get started with LibSearch in 5 minutes!

## Installation

### Prerequisites

- **Java 21** or later ([Download](https://adoptium.net/))
- **Maven 3.8+** (for building from source)

### Option 1: Use Pre-built JAR

```bash
# Run GUI mode
java -jar libsearch.jar

# Run CLI mode
java -jar libsearch.jar search "machine learning"

# Interactive REPL
java -jar libsearch.jar --interactive
```

### Option 2: Build from Source

```bash
# Clone or download the project
cd team-Se1-CSDC-HCW

# Build
mvn clean install -DskipTests

# Run
./libsearch.sh                    # Linux/macOS
libsearch.bat                     # Windows
```

## First Steps

### 1. Basic Search

**GUI Mode:**
```bash
java -jar libsearch.jar
```
- Enter "machine learning" in the search box
- Click Search
- View results in the table

**CLI Mode:**
```bash
java -jar libsearch.jar search "machine learning"
```

### 2. Advanced Search

Use filters for precise results:

```bash
# Search by author
java -jar libsearch.jar search "neural networks author:Hinton"

# Filter by year
java -jar libsearch.jar search "AI year:>2020"

# Year range
java -jar libsearch.jar search "deep learning year:2020..2024"

# Combine filters
java -jar libsearch.jar search "quantum computing author:Nielsen year:>2022 type:article"
```

### 3. Interactive REPL

For an interactive session:

```bash
java -jar libsearch.jar --interactive

# In REPL:
libsearch> search "transformer architecture" -s arxiv -n 10
libsearch> bookmark list
libsearch> index stats
libsearch> exit
```

### 4. Download PDFs

**CLI:**
```bash
# Search and save results
java -jar libsearch.jar search "attention mechanism" -f json > results.json

# Then use result URLs to download
```

**GUI:**
- Right-click on any result
- Select "Download PDF"
- Check Downloads panel for progress

### 5. Bookmarks

**Add Bookmark (GUI):**
- Right-click result → "Add Bookmark"
- Add tags and notes

**List Bookmarks (CLI):**
```bash
java -jar libsearch.jar bookmark list
```

**Find Bookmarks:**
```bash
java -jar libsearch.jar bookmark find "neural"
```

### 6. Export Citations

**BibTeX Export (CLI):**
```bash
java -jar libsearch.jar search "deep learning" -f json | \
  jq '.[] | {title, authors, year}' # Process with jq
```

**GUI:**
- Select results
- File → Export Citations
- Choose format (BibTeX, RIS, EndNote)

### 7. Offline Search

After searching online, results are automatically indexed.

**Use Offline Mode:**
```bash
# CLI
java -jar libsearch.jar search "machine learning" --offline

# GUI: View → Toggle Offline Mode
```

**Index Statistics:**
```bash
java -jar libsearch.jar index stats
```

### 8. Keyword Alerts

Monitor for new papers:

```bash
java -jar libsearch.jar monitor add \
  -n "ML Papers" \
  -k "machine learning,deep learning" \
  -a "Hinton" \
  --year-from 2024 \
  -t CONSOLE

# Check alerts
java -jar libsearch.jar monitor check-all
```

### 9. Session Tracking

View your research sessions:

```bash
# List recent sessions
java -jar libsearch.jar session list

# View activity journal
java -jar libsearch.jar session journal

# Session-specific activity
java -jar libsearch.jar session journal <session-id>
```

## Common Tasks

### Task: Find recent papers by specific author

```bash
java -jar libsearch.jar search "author:Russell year:>2023"
```

### Task: Download PDFs from search results

1. Search: `java -jar libsearch.jar search "quantum computing" -s arxiv`
2. Note the result URLs
3. Download manually or use wget/curl

### Task: Build a research library

1. Search for topics
2. Add relevant results to bookmarks (GUI)
3. Tag bookmarks by category
4. Export bookmarks to JSON/Markdown

### Task: Automated monitoring

Create a cron job (Linux/macOS):

```bash
# Edit crontab
crontab -e

# Add line (check alerts daily at 9 AM)
0 9 * * * cd /path/to/libsearch && java -jar libsearch.jar monitor check-all
```

Windows Task Scheduler:
- Create new task
- Trigger: Daily at 9:00 AM
- Action: Run `java -jar libsearch.jar monitor check-all`

## Configuration

Edit `~/.libsearch/config.yaml` to customize:

```yaml
# Example configuration
sources:
  arxiv:
    enabled: true
    max_results: 50
  pubmed:
    enabled: true
    max_results: 50

download:
  output_dir: ~/Documents/Papers
  max_concurrent: 5

search:
  max_concurrent_sources: 5
  timeout_seconds: 30
```

## Keyboard Shortcuts (GUI)

- **Ctrl+F**: Focus search field
- **Ctrl+Enter**: Execute search
- **Ctrl+B**: View bookmarks
- **Ctrl+D**: Download selected result
- **Ctrl+Q**: Quit

## Troubleshooting

**"No results found"**
- Check internet connection
- Try `java -jar libsearch.jar network diag`
- Use offline mode: `--offline`

**"Java not found"**
- Install Java 21: https://adoptium.net/
- Add to PATH

**Downloads failing**
- Some papers are behind paywalls
- Check if you're on campus network or VPN
- Try different sources

**GUI won't start**
- Ensure JavaFX is available
- Use CLI mode as alternative
- Check Java version (needs 21+)

## Next Steps

- Read [USER_GUIDE.md](USER_GUIDE.md) for complete documentation
- See [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md) for technical details
- Check [FUTURE_ROADMAP.md](FUTURE_ROADMAP.md) for upcoming features

## Getting Help

```bash
# Built-in help
java -jar libsearch.jar --help
java -jar libsearch.jar search --help
java -jar libsearch.jar bookmark --help

# Command list
java -jar libsearch.jar
```

## Examples Repository

Common search patterns:

```bash
# Recent AI ethics papers
java -jar libsearch.jar search "(AI OR \"artificial intelligence\") AND ethics year:>2023"

# COVID-19 research
java -jar libsearch.jar search "COVID-19 OR SARS-CoV-2" -s pubmed year:2023..2024

# Computer vision conferences
java -jar libsearch.jar search "\"computer vision\" type:conference year:2024"

# Open access only (when implemented)
java -jar libsearch.jar search "machine learning" --open-access

# Specific journal
java -jar libsearch.jar search "neural networks journal:Nature"
```

---

**Happy Researching!**

For questions or issues, refer to the documentation or check the logs at `~/.libsearch/logs/libsearch.log`

**Version:** 1.0.0
**Authors:** qwitch13 (nebulai13) & zahieddo
