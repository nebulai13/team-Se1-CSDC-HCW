================================================================================
                              LIBSEARCH
                Application de Recherche Académique Fédérée
================================================================================

LibSearch est un outil complet de découverte d'articles scientifiques permettant la recherche simultanée sur plusieurs sources académiques. Il propose une interface graphique JavaFX moderne et une interface en ligne de commande interactive (REPL).

================================================================================
                              FONCTIONNALITÉS
================================================================================

RECHERCHE FÉDÉRÉE
  - Recherche sur plusieurs sources académiques simultanément :
    * arXiv, PubMed, CrossRef, Semantic Scholar, Google Scholar
  - Sources institutionnelles : Primo, Réseau OBV (réseau de bibliothèques autrichien)
  - Ressources développeur : GitHub, StackOverflow, Reddit
  - Moteurs de recherche généraux : DuckDuckGo, Bing, Brave, Wikipedia

SYNTAXE DE REQUÊTE AVANCÉE
  - Syntaxe de type grep avec filtres d'auteur, plages d'années, types de fichiers
  - Opérateurs booléens (AND, OR, NOT)
  - Recherches par champ

INDEXATION PLEIN TEXTE
  - Recherche hors ligne sur les résultats mis en cache avec Apache Lucene
  - Indexation automatique des résultats de recherche
  - Indexation par champ (titre, auteurs, résumé, année, mots-clés)

TÉLÉCHARGEMENTS PDF
  - Gestion des téléchargements en file d'attente
  - Suivi de progression

FONCTIONNALITÉS DE RECHERCHE
  - Favoris avec tags et notes
  - Export de citations (BibTeX, RIS, EndNote)
  - Surveillance de mots-clés et alertes
  - Gestion des sessions et journalisation des activités

INTERFACES DOUBLES
  - Interface graphique JavaFX moderne
  - Mode CLI interactif (REPL) via Picocli/JLine3

================================================================================
                           PRÉREQUIS
================================================================================

  - Java 21 ou supérieur (Amazon Corretto recommandé)
  - Gradle 8.5 ou supérieur

================================================================================
                         INSTRUCTIONS DE BUILD
================================================================================

1. Cloner le dépôt :
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. Construire le projet :
   ./gradlew clean build

3. Créer un fat JAR (toutes dépendances incluses) :
   ./gradlew shadowJar

================================================================================
                          INSTRUCTIONS D'EXÉCUTION
================================================================================

MODE GUI
--------
Option 1 - Avec Gradle :
   ./gradlew run

Option 2 - Avec script shell :
   ./libsearch.sh

Option 3 - JAR direct (après ./gradlew shadowJar) :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar


MODE CLI
--------
Commande de recherche :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar search "machine learning"

REPL interactif :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar --interactive

Gestion des favoris :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar bookmark list

Statistiques d'index :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar index stats

Diagnostic réseau :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar network diag


EXÉCUTER LES TESTS
------------------
   ./gradlew test

================================================================================
                        GUIDE DE COMPILATION DÉTAILLÉ
================================================================================

PRÉREQUIS
---------
1. Installer Java 21+ (Amazon Corretto recommandé) :
   - macOS :    brew install --cask corretto
   - Windows :  Télécharger depuis https://aws.amazon.com/corretto/
   - Linux :    sudo apt install openjdk-21-jdk

2. Vérifier l'installation Java :
   java -version   (doit afficher la version 21 ou supérieure)

3. Gradle sera téléchargé automatiquement via le wrapper (gradlew)

ÉTAPES DE BUILD
---------------
1. Cloner et entrer dans le dossier du projet :
   git clone <repository-url>
   cd team-Se1-CSDC-HCW

2. Rendre le wrapper Gradle exécutable (Unix/macOS) :
   chmod +x gradlew

3. Construire le projet :
   ./gradlew clean build

4. Créer un fat JAR avec toutes les dépendances :
   ./gradlew shadowJar

   Sortie : build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

5. Lancer l'application :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

PROBLÈMES DE BUILD COURANTS
---------------------------
- "JAVA_HOME non défini" : Définir JAVA_HOME vers le chemin JDK 21
- Échec du téléchargement Gradle : Vérifier la connexion internet et le proxy
- JavaFX manquant : Le shadowJar inclut toutes les dépendances JavaFX

================================================================================
                          REPL (MODE INTERACTIF)
================================================================================

Démarrer le REPL interactif :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar repl

   OU avec l'option --interactive :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar -i

COMMANDES REPL
--------------
Dans le REPL, vous pouvez exécuter n'importe quelle commande sans le préfixe "libsearch" :

  libsearch> search "neural networks"
  libsearch> search "machine learning" -s arxiv,pubmed -m 20
  libsearch> bookmark list
  libsearch> bookmark list -v              (affiche URLs, notes, tags)
  libsearch> bookmark find ai              (trouve les favoris tagués "ai")
  libsearch> bookmark delete <id>
  libsearch> session list
  libsearch> session create "Ma Recherche"
  libsearch> index build
  libsearch> index status
  libsearch> network diag
  libsearch> clear                         (efface l'écran)
  libsearch> exit                          (quitte le REPL)

OPTIONS DE RECHERCHE DANS LE REPL
---------------------------------
  -s, --sources     Sources séparées par des virgules (arxiv,pubmed,crossref,scholar)
  -m, --max-results Résultats max par source (défaut : 50)
  -o, --offline     Recherche uniquement dans l'index local
  -a, --author      Filtrer par nom d'auteur
  -y, --year        Filtrer par année ou plage (2023 ou 2020-2024)
  -f, --format      Format de sortie : table, json, simple

EXEMPLES :
  search "deep learning" -s arxiv -m 10 -f json
  search "CRISPR" -s pubmed,crossref -y 2020-2024
  search "machine learning" -o                    (recherche hors ligne)

================================================================================
                             GUIDE D'UTILISATION GUI
================================================================================

LANCER LA GUI
-------------
Démarrer sans argument pour lancer la GUI :
   java -jar build/libs/team-Se1-CSDC-HCW-1.0-SNAPSHOT-all.jar

   OU avec Gradle :
   ./gradlew run

SECTIONS PRINCIPALES DE L'INTERFACE
-----------------------------------
1. PANNEAU DE RECHERCHE (haut)
   - Entrer la requête de recherche
   - Sélectionner les sources : arXiv, PubMed, CrossRef, Semantic Scholar
   - Définir la plage d'années
   - Choisir le nombre max de résultats par source
   - Activer "Mode hors ligne" pour la recherche locale

2. TABLEAU DES RÉSULTATS (centre)
   - Affiche les résultats avec colonnes : Titre, Auteurs, Année, Source, Accès
   - Cliquer sur les en-têtes pour trier
   - Double-clic sur une ligne pour ouvrir l'URL de l'article
   - Clic droit pour menu contextuel : Ouvrir URL, Télécharger PDF, Favoris, etc.

3. BARRE LATÉRALE (gauche)
   - Affiche le statut de connexion des sources (vert = en ligne, rouge = hors ligne)
   - Affiche le nom de la session et le nombre de résultats

4. BARRE DE STATUT (bas)
   - Affiche le statut, l'indicateur de progression et le temps de recherche

OPTIONS DE MENU
---------------
Menu Fichier :
  - Nouvelle session       Créer une nouvelle session
  - Ouvrir session         Charger une session précédente
  - Exporter résultats     Exporter les résultats en CSV
  - Quitter                Fermer l'application

Menu Recherche :
  - Nouvelle recherche     Focaliser le champ de recherche
  - Recherche avancée      Ouvrir les options avancées
  - Effacer résultats      Vider les résultats
  - Historique             Voir et relancer les recherches précédentes

Menu Favoris :
  - Ajouter aux favoris    Ajouter les résultats sélectionnés
  - Gérer les favoris      Voir, filtrer, supprimer les favoris

Menu Outils :
  - Gestionnaire de téléchargements   Gérer les téléchargements PDF
  - Gestionnaire d'index             Infos sur l'index local
  - Paramètres                       Préférences de l'application

Menu Aide :
  - Guide utilisateur      Aide sur la syntaxe de recherche et raccourcis
  - À propos               Infos version et application

RACCOURCIS CLAVIER
------------------
  Ctrl+N    Nouvelle recherche (champ de recherche)
  Ctrl+F    Focaliser le champ de recherche
  Ctrl+B    Ajouter aux favoris
  Ctrl+E    Exporter les résultats
  Entrée    Lancer la recherche (dans le champ)

SYNTAXE DE RECHERCHE
--------------------
  Simple :       machine learning
  Phrase :       "deep learning"
  Auteur :       author:Smith  OU  author:"John Smith"
  Année :        year:2023     OU  year:2020-2024
  Booléen :      neural AND networks
                 AI OR ML
                 cancer NOT treatment

AJOUTER AUX FAVORIS
-------------------
1. Cliquer sur l'étoile ★ dans la colonne Actions
2. OU clic droit et "Ajouter aux favoris"
3. OU sélectionner plusieurs résultats et utiliser le menu Favoris
4. Gérer les favoris via le menu

EXPORTER LES DONNÉES
--------------------
1. Fichier > Exporter résultats enregistre en CSV
2. Clic droit > Exporter citation pour BibTeX/RIS/EndNote
3. Copier le DOI via le menu contextuel

================================================================================
                         STRUCTURE DU PROJET
================================================================================

src/main/java/com/example/teamse1csdchcw/
|
+-- domain/                    Modèles de données (POJOs, enums)
|   +-- search/               SearchResult, SearchQuery, AcademicPaper
|   +-- source/               Enum SourceType
|   +-- user/                 Modèle Favori
|   +-- monitoring/           Configuration Alert
|   +-- export/               Enum ExportFormat
|
+-- service/                  Logique métier
|   +-- search/               FederatedSearchService, QueryParserService
|   +-- connector/            Connecteurs de sources + factory
|   +-- index/                IndexService (Lucene), LocalSearchService
|   +-- download/             DownloadService (gestion de file)
|   +-- bookmark/             BookmarkService
|   +-- export/               CitationExportService
|   +-- alert/                AlertService
|   +-- monitoring/           MonitoringService, NotificationService
|   +-- session/              SessionService, JournalService
|   +-- network/              ProxyConfiguration, NetworkDiagnostics
|
+-- repository/               Accès aux données (SQLite)
|   +-- sqlite/               SQLiteConnection, DatabaseInitializer
|
+-- ui/                       Interface JavaFX
|   +-- controller/           MainController, SearchController
|   +-- component/            Composants réutilisables
|   +-- model/                Modèles JavaFX
|
+-- cli/                      Interface en ligne de commande
|   +-- LibSearchCLI.java     Commande principale
|   +-- SearchCommand.java
|   +-- BookmarkCommand.java
|   +-- IndexCommand.java
|   +-- NetworkCommand.java
|   +-- MonitorCommand.java
|   +-- SessionCommand.java
|   +-- ReplCommand.java
|
+-- config/                   Gestion de configuration
+-- exception/                Exceptions personnalisées
+-- util/                     Utilitaires (HTTP, fichier, parseur, validation)

src/main/resources/
|
+-- config/application.yaml   Configuration par défaut
+-- db/schema.sql             Schéma SQLite
+-- com/example/teamse1csdchcw/
    +-- fxml/                 Layouts FXML
    +-- css/main.css          Style

================================================================================
                            TECHNOLOGIES
================================================================================

Langage & Build :
  - Java 21 (Amazon Corretto)
  - Gradle 8.11.1

Framework UI :
  - JavaFX 21.0.6
  - ControlsFX, FormsFX, ValidatorFX, TilesFX, BootstrapFX

Librairies backend :
  - OkHttp3 4.12.0         Client HTTP
  - Jsoup 1.17.2           Parsing HTML
  - Apache Lucene 9.9.1    Recherche plein texte
  - SQLite JDBC 3.45.0     Base de données
  - Jackson 2.16.1         Traitement JSON/YAML
  - JBibTeX 1.0.18         Format citation

Framework CLI :
  - Picocli 4.7.5          Parsing commandes
  - JLine3 3.25.1          Terminal UI

Logging :
  - SLF4J 2.0.11
  - Logback 1.4.14

Tests :
  - JUnit 5.12.1
  - Mockito 5.8.0

================================================================================
                           CONFIGURATION
================================================================================

Fichier de configuration : ~/.libsearch/config.yaml

Paramètres clés :
  - Activation/désactivation des sources et clés API
  - Paramètres de recherche (sources concurrentes max, timeout, rate limiting)
  - Dossier de téléchargement et limite de téléchargements simultanés
  - Dossier d'index et auto-indexation
  - Chemin de la base de données et mode WAL

Base de données : ~/.libsearch/data/libsearch.db

Tables :
  - sessions          Suivi des sessions de recherche
  - search_history    Historique des requêtes
  - search_results    Résultats mis en cache
  - bookmarks         Favoris avec tags/notes
  - alerts            Configuration de surveillance
  - alert_matches     Déclencheurs d'alerte
  - downloads         Suivi des téléchargements
  - journal           Journal d'activité
  - config            Paramètres de configuration

================================================================================
                              AUTEURS
================================================================================

  - qwitch13 (nebulai13)
  - zahieddo

================================================================================
                              VERSION
================================================================================

Version : 1.0.0

================================================================================
