module ChatWS.Server.main {
    requires ChatWS.Common.main;
    requires org.apache.tomcat.embed.core;
    requires org.apache.tomcat.embed.websocket;
    requires org.apache.tomcat.embed.el;
    requires java.desktop;
    requires java.instrument; // wird benötigt damit Tomcat Zugriff auf den ChatServer hat
    exports de.medieninformatik.server;
}
