================================================================================
                              LIBSEARCH
                Akademische Föderierte Suchanwendung
================================================================================

LibSearch ist ein umfassendes Tool zur Entdeckung von wissenschaftlichen Arbeiten, das die gleichzeitige Suche über mehrere akademische Quellen ermöglicht. Es bietet sowohl eine moderne JavaFX-GUI als auch eine interaktive Kommandozeilenschnittstelle (REPL).

================================================================================
                              FUNKTIONEN
================================================================================

FÖDERIERTE SUCHE
  - Suche in mehreren akademischen Quellen gleichzeitig:
    * arXiv, PubMed, CrossRef, Semantic Scholar, Google Scholar
  - Institutionelle Quellen: Primo, OBV Netzwerk (österreichisches Bibliotheksnetz)
  - Entwicklerressourcen: GitHub, StackOverflow, Reddit
  - Allgemeine Suchmaschinen: DuckDuckGo, Bing, Brave, Wikipedia

ERWEITERTE ABFRAGESYNTAX
  - Grep-ähnliche Syntax mit Autorenfiltern, Jahresbereichen, Dateitypen
  - Boolesche Operatoren (AND, OR, NOT)
  - Feldspezifische Suchen

VOLLTEXT-INDEXIERUNG
  - Offline-Suche auf zwischengespeicherten Ergebnissen mit Apache Lucene
  - Automatische Indexierung der Suchergebnisse
  - Feldspezifische Indexierung (Titel, Autoren, Abstract, Jahr, Schlagwörter)

PDF-DOWNLOADS
  - Warteschlangenbasiertes Bulk-Download-Management
  - Fortschrittsanzeige

RESEARCH-FEATURES
  - Lesezeichen mit Tags und Notizen
  - Zitations-Export (BibTeX, RIS, EndNote)
  - Schlagwortüberwachung und Benachrichtigungen
  - Sitzungsmanagement und Aktivitätsprotokollierung

DUALE SCHNITTSTELLEN
  - Moderne JavaFX-GUI
  - Interaktiver CLI (REPL) Modus via Picocli/JLine3

================================================================================
                           VORAUSSETZUNGEN
================================================================================

  - Java 21 oder höher (Amazon Corretto empfohlen)
  - Gradle 8.5 oder höher

================================================================================
                         BUILD-ANLEITUNG
================================================================================

1. Repository klonen:
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. Projekt bauen:
   ./gradlew clean build

3. Fat JAR erstellen (alle Abhängigkeiten enthalten):
   ./gradlew shadowJar

================================================================================
                          AUSFÜHRUNG
================================================================================

GUI MODUS
---------
Option 1 - Mit Gradle:
   ./gradlew run

Option 2 - Mit Shell-Skript:
   ./libsearch.sh

Option 3 - Direktes JAR (nach ./gradlew shadowJar):
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar


CLI MODUS
---------
Suchbefehl:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar search "machine learning"

Interaktives REPL:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar --interactive

Lesezeichenverwaltung:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar bookmark list

Indexstatistiken:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar index stats

Netzwerkdiagnose:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar network diag


TESTS AUSFÜHREN
---------------
   ./gradlew test

================================================================================
                        DETAILLIERTE KOMPILIERUNGSANLEITUNG
================================================================================

VORAUSSETZUNGEN
---------------
1. Java 21+ installieren (Amazon Corretto empfohlen):
   - macOS:    brew install --cask corretto
   - Windows:  Download von https://aws.amazon.com/corretto/
   - Linux:    sudo apt install openjdk-21-jdk

2. Java-Installation prüfen:
   java -version   (sollte Version 21 oder höher anzeigen)

3. Gradle wird automatisch über das Wrapper-Skript (gradlew) heruntergeladen

BUILD-SCHRITTE
--------------
1. Projekt klonen und Verzeichnis betreten:
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. Gradle-Wrapper ausführbar machen (Unix/macOS):
   chmod +x gradlew

3. Projekt bauen:
   ./gradlew clean build

4. Fat JAR mit allen Abhängigkeiten erstellen:
   ./gradlew shadowJar

   Ausgabe: build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

5. Anwendung starten:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

HÄUFIGE BUILD-PROBLEME
----------------------
- "JAVA_HOME nicht gesetzt": JAVA_HOME auf den JDK 21-Pfad setzen
- Gradle-Download schlägt fehl: Internetverbindung und Proxy prüfen
- JavaFX fehlt: Das shadowJar enthält alle JavaFX-Abhängigkeiten

================================================================================
                          REPL (INTERAKTIVER MODUS)
================================================================================

Interaktives REPL starten:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar repl

   ODER mit dem --interactive-Flag:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar -i

REPL-BEFEHLE
------------
Im REPL können Sie jeden Befehl ohne das "libsearch"-Präfix ausführen:

  libsearch> search "neural networks"
  libsearch> search "machine learning" -s arxiv,pubmed -m 20
  libsearch> bookmark list
  libsearch> bookmark list -v              (zeigt URLs, Notizen, Tags)
  libsearch> bookmark find ai              (findet Lesezeichen mit Tag "ai")
  libsearch> bookmark delete <id>
  libsearch> session list
  libsearch> session create "Meine Forschung"
  libsearch> index build
  libsearch> index status
  libsearch> network diag
  libsearch> clear                         (Bildschirm leeren)
  libsearch> exit                          (REPL beenden)

SUCHOPTIONEN IM REPL
--------------------
  -s, --sources     Kommagetrennte Quellen (arxiv,pubmed,crossref,scholar)
  -m, --max-results Maximale Ergebnisse pro Quelle (Standard: 50)
  -o, --offline     Nur lokalen Index durchsuchen
  -a, --author      Nach Autor filtern
  -y, --year        Nach Jahr oder Bereich filtern (2023 oder 2020-2024)
  -f, --format      Ausgabeformat: table, json, simple

BEISPIELE:
  search "deep learning" -s arxiv -m 10 -f json
  search "CRISPR" -s pubmed,crossref -y 2020-2024
  search "machine learning" -o                    (Offline-Suche)

================================================================================
                             GUI-BENUTZERHANDBUCH
================================================================================

GUI STARTEN
-----------
Ohne Argumente starten, um die GUI zu öffnen:
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

   ODER mit Gradle:
   ./gradlew run

HAUPTBEREICHE DER OBERFLÄCHE
----------------------------
1. SUCHPANEL (oben)
   - Suchbegriff eingeben
   - Quellen auswählen: arXiv, PubMed, CrossRef, Semantic Scholar
   - Jahresbereich einstellen
   - Maximale Ergebnisse pro Quelle wählen
   - "Offline-Modus" aktivieren für lokale Suche

2. ERGEBNISTABELLE (Mitte)
   - Zeigt Suchergebnisse mit Spalten: Titel, Autoren, Jahr, Quelle, Zugang
   - Spaltenüberschriften zum Sortieren anklicken
   - Doppelklick auf eine Zeile öffnet die Paper-URL
   - Rechtsklick für Kontextmenü: URL öffnen, PDF herunterladen, Lesezeichen, usw.

3. SIDEBAR (links)
   - Zeigt Verbindungsstatus der Quellen (grün = online, rot = offline)
   - Zeigt aktuellen Sitzungsnamen und Ergebnisanzahl

4. STATUSLEISTE (unten)
   - Zeigt aktuellen Status, Fortschrittsanzeige und Suchzeit

MENÜOPTIONEN
------------
Datei-Menü:
  - Neue Sitzung       Neue Suche starten
  - Sitzung öffnen     Frühere Sitzung laden
  - Ergebnisse exportieren   Ergebnisse als CSV speichern
  - Beenden            Anwendung schließen

Such-Menü:
  - Neue Suche         Suchfeld fokussieren
  - Erweiterte Suche   Erweiterte Optionen öffnen
  - Ergebnisse löschen Ergebnisse leeren
  - Suchverlauf        Frühere Suchen anzeigen und wiederholen

Lesezeichen-Menü:
  - Lesezeichen setzen   Ausgewählte Ergebnisse speichern
  - Lesezeichen verwalten   Lesezeichen anzeigen, filtern, löschen

Tools-Menü:
  - Download-Manager      PDF-Downloads verwalten
  - Index-Manager         Informationen zum lokalen Index
  - Einstellungen         Anwendungseinstellungen

Hilfe-Menü:
  - Benutzerhandbuch      Suchsyntax und Shortcuts
  - Über                  Versions- und App-Info

TASTENKÜRZEL
------------
  Ctrl+N    Neue Suche (Suchfeld)
  Ctrl+F    Suchfeld fokussieren
  Ctrl+B    Lesezeichen setzen
  Ctrl+E    Ergebnisse exportieren
  Enter     Suche ausführen (im Suchfeld)

SUCHSYNTAX
----------
  Einfach:       machine learning
  Phrase:        "deep learning"
  Autor:         author:Smith  ODER  author:"John Smith"
  Jahr:          year:2023     ODER  year:2020-2024
  Boolesch:      neural AND networks
                 AI OR ML
                 cancer NOT treatment

LESEZEICHEN
-----------
1. Auf das ★-Symbol in der Aktionsspalte klicken
2. ODER Rechtsklick und "Lesezeichen" wählen
3. ODER mehrere Ergebnisse auswählen und Lesezeichen-Menü nutzen
4. Lesezeichen über das Menü verwalten

EXPORTIEREN
-----------
1. Datei > Ergebnisse exportieren speichert als CSV
2. Rechtsklick > Zitation exportieren für BibTeX/RIS/EndNote
3. DOI per Rechtsklick kopieren

================================================================================
                         PROJEKTSTRUKTUR
================================================================================

src/main/java/com/example/teamse1csdchcw/
|
+-- domain/                    Datenmodelle (POJOs, Enums)
|   +-- search/               SearchResult, SearchQuery, AcademicPaper
|   +-- source/               SourceType Enum
|   +-- user/                 Bookmark Modell
|   +-- monitoring/           Alert-Konfiguration
|   +-- export/               ExportFormat Enum
|
+-- service/                  Business-Logik
|   +-- search/               FederatedSearchService, QueryParserService
|   +-- connector/            Quellen-Connectoren + Factory
|   +-- index/                IndexService (Lucene), LocalSearchService
|   +-- download/             DownloadService (Warteschlange)
|   +-- bookmark/             BookmarkService
|   +-- export/               CitationExportService
|   +-- alert/                AlertService
|   +-- monitoring/           MonitoringService, NotificationService
|   +-- session/              SessionService, JournalService
|   +-- network/              ProxyConfiguration, NetworkDiagnostics
|
+-- repository/               Datenzugriff (SQLite)
|   +-- sqlite/               SQLiteConnection, DatabaseInitializer
|
+-- ui/                       JavaFX-GUI
|   +-- controller/           MainController, SearchController
|   +-- component/            Wiederverwendbare UI-Komponenten
|   +-- model/                JavaFX-Modelle
|
+-- cli/                      Kommandozeile
|   +-- LibSearchCLI.java     Haupt-CLI-Befehl
|   +-- SearchCommand.java
|   +-- BookmarkCommand.java
|   +-- IndexCommand.java
|   +-- NetworkCommand.java
|   +-- MonitorCommand.java
|   +-- SessionCommand.java
|   +-- ReplCommand.java
|
+-- config/                   Konfigurationsmanagement
+-- exception/                Eigene Exceptions
+-- util/                     Utilities (HTTP, Datei, Parser, Validierung)

src/main/resources/
|
+-- config/application.yaml   Standardkonfiguration
+-- db/schema.sql             SQLite-Schema
+-- com/example/teamse1csdchcw/
    +-- fxml/                 FXML-Layouts
    +-- css/main.css          Styling

================================================================================
                            TECHNOLOGIEN
================================================================================

Sprache & Build:
  - Java 21 (Amazon Corretto)
  - Gradle 8.11.1

UI-Framework:
  - JavaFX 21.0.6
  - ControlsFX, FormsFX, ValidatorFX, TilesFX, BootstrapFX

Backend-Bibliotheken:
  - OkHttp3 4.12.0         HTTP-Client
  - Jsoup 1.17.2           HTML-Parsing
  - Apache Lucene 9.9.1    Volltextsuche
  - SQLite JDBC 3.45.0     Datenbank
  - Jackson 2.16.1         JSON/YAML-Verarbeitung
  - JBibTeX 1.0.18         Zitationsformat

CLI-Framework:
  - Picocli 4.7.5          Kommandozeilen-Parsing
  - JLine3 3.25.1          Terminal-UI

Logging:
  - SLF4J 2.0.11
  - Logback 1.4.14

Testing:
  - JUnit 5.12.1
  - Mockito 5.8.0

================================================================================
                           KONFIGURATION
================================================================================

Konfigurationsdatei: ~/.libsearch/config.yaml

Wichtige Einstellungen:
  - Quellen aktivieren/deaktivieren und API-Keys
  - Suchparameter (max. parallele Quellen, Timeout, Rate-Limiting)
  - Download-Verzeichnis und gleichzeitige Downloads
  - Index-Verzeichnis und Auto-Indexing
  - Datenbankpfad und WAL-Modus

Datenbank: ~/.libsearch/data/libsearch.db

Tabellen:
  - sessions          Sitzungsverwaltung
  - search_history    Suchverlauf
  - search_results    Zwischengespeicherte Ergebnisse
  - bookmarks         Lesezeichen mit Tags/Notizen
  - alerts            Schlagwortüberwachung
  - alert_matches     Benachrichtigungs-Trigger
  - downloads         Download-Warteschlange
  - journal           Aktivitätenprotokoll
  - config            Konfigurationswerte

================================================================================
                              AUTOREN
================================================================================

  - qwitch13 (nebulai13)
  - zahieddo

================================================================================
                              VERSION
================================================================================

Version: 1.0.0

================================================================================
