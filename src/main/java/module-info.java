// -- java 9+ module descriptor: defines dependencies & visibility --
// -- jpms (java platform module system) enforces encapsulation --
module com.example.teamse1csdchcw {
    // JavaFX modules
    // -- javafx.controls: buttons, tables, text fields, etc --
    requires javafx.controls;
    // -- javafx.fxml: xml-based ui layout support --
    requires javafx.fxml;
    // -- javafx.web: webview component (embedded browser) --
    requires javafx.web;
    // -- javafx.swing: interop w/ swing (awt/swing embedding) --
    requires javafx.swing;

    // JavaFX UI libraries
    // -- controlsfx: extra controls like searchable combobox --
    requires org.controlsfx.controls;
    // -- formsfx: declarative form building lib --
    requires com.dlsc.formsfx;
    // -- validatorfx: input validation framework --
    requires net.synedra.validatorfx;
    // -- ikonli: icon packs (fontawesome, etc) --
    requires org.kordamp.ikonli.javafx;
    // -- bootstrapfx: bootstrap-style css for javafx --
    requires org.kordamp.bootstrapfx.core;
    // -- tilesfx: dashboard tile components --
    requires eu.hansolo.tilesfx;
    // -- fxgl: game dev framework (may be unused) --
    requires com.almasb.fxgl.all;

    // HTTP and parsing
    // -- okhttp3: modern http client for api calls --
    requires okhttp3;
    // -- okhttp logging interceptor for debug --
    requires okhttp3.logging;
    // -- jsoup: html parser for scraping/cleaning --
    requires org.jsoup;

    // Full-text search (Lucene)
    // -- lucene core: inverted index & search engine --
    requires org.apache.lucene.core;
    // -- lucene queryparser: parse user queries --
    requires org.apache.lucene.queryparser;
    // -- lucene analyzers: tokenizers, stemmers, etc --
    requires org.apache.lucene.analysis.common;

    // Database
    // -- java.sql: jdbc api for db access --
    requires java.sql;
    // -- sqlite jdbc driver --
    requires org.xerial.sqlitejdbc;

    // JSON/YAML processing
    // -- jackson databind: json <-> pojo mapping --
    requires com.fasterxml.jackson.databind;
    // -- jackson yaml: parse yaml config files --
    requires com.fasterxml.jackson.dataformat.yaml;
    // -- jackson jsr310: java.time support --
    requires com.fasterxml.jackson.datatype.jsr310;

    // CLI framework
    // -- picocli: cmd line arg parsing w/ annotations --
    requires info.picocli;
    // -- jline: readline-like input w/ history & completion --
    requires org.jline;
    // -- jansi: ansi color codes for terminal output --
    requires org.fusesource.jansi;

    // Logging
    // -- slf4j: logging facade/abstraction --
    requires org.slf4j;
    // -- logback: slf4j implementation --
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    // Utilities
    // -- commons-lang3: string utils, object utils, etc --
    requires org.apache.commons.lang3;
    // -- commons-io: file/stream utilities --
    requires org.apache.commons.io;

    // Core Java modules
    // -- java.desktop: awt/swing (needed by some libs) --
    requires java.desktop;
    // -- java.prefs: preferences api for settings --
    requires java.prefs;

    // Opens for FXML reflection
    // -- opens = allows runtime reflection access --
    // -- needed for fxmlloader to instantiate controllers --
    opens com.example.teamse1csdchcw to javafx.fxml;
    opens com.example.teamse1csdchcw.ui.controller to javafx.fxml;

    // Exports for public API
    // -- exports = makes pkg visible to other modules --
    exports com.example.teamse1csdchcw;
    exports com.example.teamse1csdchcw.domain.search;
    exports com.example.teamse1csdchcw.domain.source;
    exports com.example.teamse1csdchcw.domain.user;
    exports com.example.teamse1csdchcw.domain.export;
    exports com.example.teamse1csdchcw.service.search;
    exports com.example.teamse1csdchcw.service.connector;
    exports com.example.teamse1csdchcw.ui.controller;
}