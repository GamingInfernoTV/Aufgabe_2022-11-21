package de.medieninformatik.server;


import org.apache.catalina.LifecycleException;

import java.io.IOException;

/**
 * Hauptklasse zum Starten des Servers
 */
public class Main {
    /**
     * Ruft die Start-Methode des Servers auf
     *
     * @param args nicht benutzt
     * @throws LifecycleException ignoriert
     * @throws IOException ignoriert
     */
    public static void main(String[] args) throws LifecycleException, IOException {
        Server.start();
    }
}
