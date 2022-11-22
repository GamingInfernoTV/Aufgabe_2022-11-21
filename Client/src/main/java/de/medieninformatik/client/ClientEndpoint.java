package de.medieninformatik.client;

import de.medieninformatik.common.Message;
import jakarta.websocket.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.SynchronousQueue;

@jakarta.websocket.ClientEndpoint
public class ClientEndpoint {
    private final SynchronousQueue<Message> incomingMessage;
    private final Runnable onCloseAction;
    private final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    private Session userSession;

    public ClientEndpoint(SynchronousQueue<Message> incomingMessage, Runnable onCloseAction) {
        this.incomingMessage = incomingMessage;
        this.onCloseAction = onCloseAction;
    }

    void connect(String uri) {
        try {
            userSession = container.connectToServer(this, new URI(uri));
        } catch (DeploymentException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    void disconnect() {
        try {
            userSession.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMessage(Message message) {
        try {
            userSession.getBasicRemote().sendText(message.toString());
        } catch (IOException e) {
            try {
                userSession.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                userSession = null;
            }
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("User connected");
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        incomingMessage.offer(Message.getFromString(msg));
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("User disconnected");
        onCloseAction.run();
    }
}
