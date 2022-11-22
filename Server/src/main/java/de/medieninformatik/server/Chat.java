package de.medieninformatik.server;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat")
public class Chat {

    @OnOpen
    public void onOpen() {

    }

    @OnClose
    public void onClose() {

    }

    @OnError
    public void onError() {

    }

    @OnMessage
    public void onMessage() {

    }
}
