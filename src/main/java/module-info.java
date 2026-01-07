module com.example.teamse1csdchcw {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    // JavaFX UI libraries
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    // HTTP and parsing
    requires okhttp3;
    requires okhttp3.logging;
    requires org.jsoup;

    // Full-text search (Lucene)
    requires org.apache.lucene.core;
    requires org.apache.lucene.queryparser;
    requires org.apache.lucene.analysis.common;

    // Database
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // JSON/YAML processing
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;

    // CLI framework
    requires info.picocli;
    requires org.jline;
    requires org.fusesource.jansi;

    // Logging
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    // Utilities
    requires org.apache.commons.lang3;
    requires org.apache.commons.io;

    // Core Java modules
    requires java.desktop;
    requires java.prefs;

    // Opens for FXML reflection
    opens com.example.teamse1csdchcw to javafx.fxml;
    opens com.example.teamse1csdchcw.ui.controller to javafx.fxml;

    // Exports for public API
    exports com.example.teamse1csdchcw;
    exports com.example.teamse1csdchcw.domain.search;
    exports com.example.teamse1csdchcw.domain.source;
    exports com.example.teamse1csdchcw.domain.user;
    exports com.example.teamse1csdchcw.domain.export;
    exports com.example.teamse1csdchcw.service.search;
    exports com.example.teamse1csdchcw.service.connector;
    exports com.example.teamse1csdchcw.ui.controller;
}