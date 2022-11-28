package de.medieninformatik.client;

import de.medieninformatik.common.Message;
import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.SynchronousQueue;

@jakarta.websocket.ClientEndpoint
public class ClientEndpoint {
    private final SynchronousQueue<Message> incomingMessage;
    private final Runnable onCloseAction;
    private final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    private Session userSession;

    /**
     * Fügt die einkommende Message einer {@link SynchronousQueue} hinzu.
     * Diese ist nötig um eine Thread-Sicherheit zu gewährleisten.
     *
     * @param incomingMessage {@link Message}, die der Queue hinzugefügt werden soll
     * @param onCloseAction   Ein {@link Runnable}, welches ausgeführt wird, wenn der Client geschlossen wird
     */
    public ClientEndpoint(Queue<Message> incomingMessage, Runnable onCloseAction) {
        if (incomingMessage instanceof SynchronousQueue<Message> synchronousQueue)
            this.incomingMessage = synchronousQueue;
        else throw new IllegalArgumentException("queue must be of type SynchronousQueue");
        this.onCloseAction = onCloseAction;
    }

    /**
     * Verbindet den Client mit dem Server.
     *
     * @param uri Server URL
     */
    void connect(String uri) {
        try {
            userSession = container.connectToServer(this, new URI(uri));
        } catch (DeploymentException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Beendet die Verbindung des Clients mit dem Server
     */
    void disconnect() {
        try {
            userSession.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sendet die Message des Clients an den Server.
     * Sollte dies misslingen wird entweder die Verbindung
     * mit dem Server geschlossen oder eine Fehlermeldung ausgegeben
     *
     * @param message vom User eingegebene Message, die dem Server übergeben werden soll
     */
    void sendMessage(Message message) {
        try {
            userSession.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            try {
                userSession.close();
            } catch (IOException ex) {
                e.printStackTrace(System.err);
            } finally {
                userSession = null;
            }
        }
    }

    /**
     * Gibt die Information aus, dass sich ein neuer User mit dem
     * Server verbunden hat, sobald die Verbindung aufgebaut wurde
     *
     * @param session nicht genutzt
     */
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("User connected");
    }

    /**
     * Übergibt die eingehende Nachricht der gespeicherten {@link SynchronousQueue}
     *
     * @param session nicht benutzt
     * @param msg     Die vom Server eingehende Methode
     */
    @OnMessage
    public void onMessage(Session session, String msg) {
        try {
            incomingMessage.put(Message.getFromString(msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            try {
                userSession.close();
            } catch (IOException ex) {
                e.printStackTrace(System.err);
            } finally {
                userSession = null;
                onCloseAction.run();
            }
        }
    }

    /**
     * Stoppt den Client, wenn das Fenster des Clients geschlossen wird
     * und gibt aus, dass der User disconnected ist.
     *
     * @param session nicht genutzt
     */
    @OnClose
    public void onClose(Session session) {
        System.out.println("User disconnected");
        onCloseAction.run();
    }
}
