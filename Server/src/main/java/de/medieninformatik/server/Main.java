package de.medieninformatik.server;


import org.apache.catalina.LifecycleException;

import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws LifecycleException, UnknownHostException {
        Server.start(8080);
    }
}
