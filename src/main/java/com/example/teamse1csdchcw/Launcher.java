package com.example.teamse1csdchcw;

import javafx.application.Application;

import java.nio.file.Files;
import java.nio.file.Path;
import com.example.teamse1csdchcw.repository.sqlite.DatabaseInitializer;


public class Launcher {
    public static void main(String[] args) {
        ensureAppDirectories();
        DatabaseInitializer.initialize();
        Application.launch(HelloApplication.class, args);

    }

    private static void ensureAppDirectories() {
        try {
            Path base = Path.of(System.getProperty("user.home"), ".libsearch");
            Files.createDirectories(base.resolve("logs"));
            Files.createDirectories(base.resolve("data"));
            Files.createDirectories(base.resolve("index"));
        } catch (Exception ignored) {
            // absichtlich ohne Logging: Logback ist hier evtl. noch nicht initialisiert
        }
    }
}
