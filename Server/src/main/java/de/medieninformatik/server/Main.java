package de.medieninformatik.server;


import org.apache.catalina.LifecycleException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws LifecycleException, IOException {
        Server.start();
    }
}
