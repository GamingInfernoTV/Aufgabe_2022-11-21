package de.medieninformatik.client;

import javafx.application.Application;

/**
 * Hauptklasse zum Starten der {@link Client}-{@link Application}
 */
public class Main {
    /**
     * Startet die Application
     * @param args nicht benutzt
     */
    public static void main(String[] args) {
        Application.launch(Client.class, args);
    }
}
