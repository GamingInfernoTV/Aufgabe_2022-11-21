package de.medieninformatik.server;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    private static final int PORT = 8080;

    private Server() {
    }

    /**
     * Startet den Server auf dem vorher definierten Port.
     * Zudem wird eine URL des Servers erstellt.
     *
     * @throws LifecycleException Exception
     * @throws IOException Exception
     */
    static void start() throws LifecycleException, IOException {
        String webapps = "Chat";
        String doc = "web";

        Logger.getLogger("").setLevel(Level.SEVERE);

        Tomcat tomcat = new Tomcat();
        final String tmpDir = System.getProperty("java.io.tmpdir");
        tomcat.setBaseDir(tmpDir);
        Path docBase = Paths.get(doc).toAbsolutePath();
        Context ctx = tomcat.addWebapp(webapps, docBase.toString());

        Connector con = new Connector();
        con.setPort(PORT);

        Service service = tomcat.getService();
        service.addConnector(con);

        tomcat.start();
        System.out.printf("Doc-base: %s%n", ctx.getDocBase());
        String url = con.getScheme() + "://" +
                InetAddress.getLocalHost().getHostAddress() + ":" +
                con.getPort() + ctx.getPath();
        System.out.printf("URL: %s%n", url);

        // noinspection ResultOfMethodCallIgnored
        System.in.read();

        tomcat.stop();
        tomcat.destroy();
    }
}
