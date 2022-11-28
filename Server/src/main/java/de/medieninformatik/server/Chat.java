package de.medieninformatik.server;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint("/chat")
public class Chat {
    private static final List<Session> connections = new CopyOnWriteArrayList<>();

    /**
     * Startet die Session, wenn der Server geöffnet wird
     *
     * @param session aktuelle Session
     */
    @OnOpen
    public void onOpen(Session session) {
        System.out.printf("%s: called onOpen%n", session.getId());
        connections.add(session);
    }

    /**
     * Entfernt den Server aus der Session
     *
     * @param session aktuelle Session
     */
    @OnClose
    public void onClose(Session session) {
        System.out.printf("%s: called onClose%n", session.getId());
        connections.remove(session);
    }

    /**
     * Gibt bei einem Fehler eine Fehlermeldung aus,
     * samt der fehlerhaften ID
     * @param session aktuelle Session
     * @param error aufgetretener Fehler
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.printf("%s: %s%n", session.getId(), error.getMessage());
    }

    /**
     * Sendet die einkommende Message an alle verbundenen Clients
     * der Session
     *
     * @param session aktuelle Session
     * @param msg eingehende Message
     */
    @OnMessage
    public void onMessage(Session session, String msg) {
        System.out.printf("%s: %s%n", session.getId(), msg);
        connections.forEach(s -> {
            try {
                s.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                connections.remove(s);
                try {
                    s.close();
                } catch (IOException ex) {
                    e.printStackTrace(System.err);
                }
            }
        });
    }
}
