module de.medieninformatik.client {
    requires de.medieninformatik.common;
    requires org.apache.tomcat.embed.websocket;
    requires java.net.http;
    requires javafx.controls;
    requires java.instrument; // wird benötigt damit Tomcat Zugriff auf den ChatServer hat
    exports de.medieninformatik.client;
}