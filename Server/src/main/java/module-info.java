module de.medieninformatik.server {
    requires de.medieninformatik.common;
    requires org.apache.tomcat.embed.core;
    requires org.apache.tomcat.embed.websocket;
    requires org.apache.tomcat.embed.el;
    requires java.desktop;
    requires java.instrument; // wird benötigt damit Tomcat Zugriff auf den ChatServer hat
    exports de.medieninformatik.server;
}
