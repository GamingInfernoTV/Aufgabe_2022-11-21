package de.medieninformatik.client;

import de.medieninformatik.common.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author jurgen
 * date 2020-11-04
 * @author Malte Kasolowsky <code>m30114</code>
 * @author Aaron Pöhlmann <code>m30115</code>
 * @version 2.0 <span>
 * <b>Version 2.0</b></br>
 * Wie in der vorherigen Version stellt der Client den Chat zur Verfügung.
 * Es wurden jedoch veränderungen an der Funktionsweise einzelner
 * Methoden vorgenommen.
 * Zudem wurden mehrere Funktionen auf einzelne Methoden und Klassen aufgeteilt.
 * Weiterhin können mehrere Clients gleichzeitig laufen.
 * Allerdings werden jetzt Threads genutzt.
 * </span>
 * <p>
 * <span>
 * <b>Vorherige Versionsbeschreibung:</b><br />
 * Der Client stellt das Nutzer-Interface für den Chat zur Verfügung.
 * Die Nachrichten vom Server werden mit einem JavaFX-Service empfangen.
 * Die lokalen Eingaben werden jedoch direkt (ohne Threads) an den Server
 * gesendet. Es können mehrere Clients zugleich laufen.
 * <p>
 * Das Fenster besteht aus 3 Bereichen:
 * 1.das Verlaufs-Fenster gibt den Chat-Verlauf seit dem Einlogen wieder.
 * Ebenso werden Meldungen zum Ein-/und Ausloggen angezeigt.
 * <p>
 * 2. Die Eingabezeile hat zwei Funktionen:
 * a) Vor dem Einloggen  wird hier der Nutzername angegeben (ohne die
 * Return-Taste zu drücken).
 * b) Nach dem einloggen werden hier die Chat-Nachrichten an den Server
 * eingegeben. Durch drücken der Return-taste wird die Nachricht an
 * den Server geschickt.
 * <p>
 * 3. Der Eingabeknopf:
 * a) Falls der Nutzer noch nicht eingeloggt ist, dann wird durch drücken
 * des Knopfes der Text in der Eingabezeile als Nutzername interpretiert
 * und dieser Nutzer am Server angemeldet.
 * b) Falls der Nutzer angemeldet ist, wird er durch drücken des Knopfes
 * abgemeldet.
 * </p>
 * </span>
 */
public class Client extends Application {
    private static final int HEIGHT = 500;
    private static final int WIDTH = 800;
    private static final int FONT_SIZE = 14;
    private static final String FONT = " -fx-font: " + FONT_SIZE + "pt \"Arial\";";
    private static final String BG_GRAY = " -fx-background-color: lightgray;";
    private static final String BG_RED = " -fx-background-color: red;";
    private String host;
    private int port;
    private Stage stage;
    private Button loginButton;
    private TextField eingabeZeile;
    private TextArea verlauf;
    private String user;
    private AtomicBoolean isLoggedIn; // in JavaFX-Thread und in Task
    private Semaphore semaphore; // verhindert cleanup-run bevor letzte msg gesendet
    private Service<Void> service;
    private Service<Void> sendService;
    private BlockingQueue<Message> messages;
    private SynchronousQueue<Message> incomingMessage;
    private ClientEndpoint clientEndpoint;

    /**
     * Bereitet JavaFX vor (ohne GUI-Elemente).
     * Auf der Kommandozeile des Clients kann
     * der Host und der Port mit
     * java client --host=localhost --port=8080
     * übergeben werden.
     */
    @Override
    public void init() {
        Parameters p = this.getParameters();
        Map<String, String> map = p.getNamed();
        host = map.getOrDefault("host", "localhost");
        port = Integer.parseInt(map.getOrDefault("port", "8080"));
        isLoggedIn = new AtomicBoolean(false);
        semaphore = new Semaphore(1);
        messages = new LinkedBlockingQueue<>();
        incomingMessage = new SynchronousQueue<>();
        clientEndpoint = new ClientEndpoint(incomingMessage, () -> isLoggedIn.set(false));
    }

    /**
     * Erzeugt das GUI.
     *
     * @param stage stage
     */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        eingabeZeile = new TextField();
        eingabeZeile.setStyle(FONT);
        // Wenn return gedrückt, dann rufe Eventhandler sendenachricht auf
        eingabeZeile.setOnAction(this::sendeNachricht);

        verlauf = new TextArea();
        verlauf.setWrapText(true);
        verlauf.setStyle(FONT);
        verlauf.setEditable(false);
        verlauf.setPrefHeight(HEIGHT - 6d * FONT_SIZE);
        verlauf.setPrefWidth(WIDTH - 20d);

        loginButton = new Button("Anmelden");
        loginButton.setStyle(FONT + BG_GRAY);
        loginButton.setOnAction(this::handleButton);

        final VBox vbox = new VBox();
        final ScrollPane pane = new ScrollPane(verlauf);
        final Button sendButton = new Button("Senden");
        final HBox hbox = new HBox(loginButton, sendButton);
        sendButton.setStyle(FONT + BG_GRAY);
        sendButton.setOnAction(this::sendeNachricht);
        hbox.setPadding(new Insets(15));
        hbox.setSpacing(15);
        vbox.getChildren().addAll(pane, eingabeZeile, hbox);
        stage.setScene(new Scene(vbox, WIDTH, HEIGHT));
        stage.setTitle("ChatClient");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> {
            logout();
            Platform.exit();
        });
        stage.show();
    }

    /**
     * Button dient sowohl zum login als auch zum logout.
     *
     * @param e e
     */
    private void handleButton(ActionEvent e) {
        if (isLoggedIn.get()) logout();
        else {
            user = eingabeZeile.getText();
            if (user.isBlank()) new Alert(
                    Alert.AlertType.ERROR,
                    "Nutzername darf nicht leer sein",
                    ButtonType.OK
            ).show();
            else login();
        }
    }

    /**
     * Melde Benutzer auf dem Server an.
     */
    private void login() {
        try {
            clientEndpoint.connect("ws://%s:%d/Chat/chat".formatted(host, port));

            sendService = new Service<>() {
                @Override
                protected Task<Void> createTask() {
                    return new SendTask();
                }
            };
            service = new Service<>() {
                @Override
                protected Task<Void> createTask() {
                    return new ChatTask();
                }
            };
            // cleanup wartet auf freie semaphoren von ChatTask
            sendService.setOnSucceeded(this::cleanup);
            sendService.setOnFailed(this::cleanup);
            isLoggedIn.set(true); // muss vor service.start stehen
            service.start();
            sendService.start();

            loginButton.setText("Abmelden");
            loginButton.setStyle(FONT + BG_RED);

            offerMessage(new Message(Message.Action.JOIN, user));
            stage.setTitle("ChatClient -- " + user);
        } finally {
            eingabeZeile.setText("");
            eingabeZeile.requestFocus();
        }
    }

    /**
     * Melde Nutzer vom Server ab
     */
    private void logout() {
        if (isLoggedIn.get()) {
            offerMessage(new Message(Message.Action.LEAVE, user));
            loginButton.setText("Anmelden");
            loginButton.setStyle(FONT + BG_GRAY);
        }
    }

    /**
     * Bereinigt die Nachrichten des Clients
     * @param e ignoriert
     */
    private void cleanup(WorkerStateEvent e) {
        try {
            semaphore.acquire(); // warte bis ChatTask logout von Server hat
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            interrupted.printStackTrace(System.err);
        }
        clientEndpoint.disconnect();
        messages.clear();
        sendService = null;
        service = null;
        user = null;
        eingabeZeile.setText("");
        eingabeZeile.requestFocus();
        stage.setTitle("ChatClient");
        semaphore.release();
    }

    /**
     * Schreibt den Inhalt der Eingabezeile als Nachricht in
     * die Sende-Warteschlange.
     * Event wird ausgelöst, wenn Nutzer eingeloggt ist und
     * Return auf Eingabezeile gedrückt wird.
     *
     * @param event ignoriert
     */
    private void sendeNachricht(ActionEvent event) {
        if (!isLoggedIn.get()) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Nutzer ist nicht angemeldet",
                    ButtonType.OK
            ).show();
            return;
        }
        String input = eingabeZeile.getText();
        if (input.isBlank()) {
            new Alert(
                    Alert.AlertType.ERROR,
                    "Eingegebene Nachricht darf nicht leer sein!",
                    ButtonType.OK
            ).show();
        } else {
            offerMessage(new Message(Message.Action.SEND, user, input));
            eingabeZeile.setText("");
            eingabeZeile.requestFocus();
        }
    }

    /**
     * Die vom Service ausgeführte Task.
     * Sie nimmt vom Server Nachrichten entgegen,
     * gibt diese aus und reagiert gegebenenfalls.
     * <p>
     * Falls eine LEAVE-Nachricht des Nutzers eintrifft,
     * wird die Tasks und damit der Service beendet.
     */
    private class ChatTask extends Task<Void> {
        @Override
        protected Void call() throws Exception {
            semaphore.acquire(); // Verhindert zu frühen cleanup
            while (isLoggedIn.get()) {
                Message msg = incomingMessage.take();
                Message.Action action = msg.action();
                final String ausgabe = switch (action) {
                    case JOIN -> String.format(">>> %s ist angemeldet%n", msg.user());
                    case SEND -> String.format("%s: %s%n", msg.user(), msg.content());
                    case LEAVE -> String.format("<<< %s ist abgemeldet%n", msg.user());
                };
                Platform.runLater(() -> verlauf.appendText(ausgabe));
                if (action == Message.Action.LEAVE && user.equals(msg.user())) isLoggedIn.set(false);
            }
            semaphore.release();
            return null;
        }
    }

    /**
     * Wartet auf Nachricht in Warteschlange.
     * Diese Nachricht wird dann an den Server gesendet.
     * LEAVE-Nachricht oder isLoggedIn == false beendet die Task
     */
    private class SendTask extends Task<Void> {
        @Override
        protected Void call() throws Exception {
            while (isLoggedIn.get()) {
                Message msg = messages.take(); // Blockiert
                clientEndpoint.sendMessage(msg);
                if (msg.action() == Message.Action.LEAVE) break;
            }
            return null;
        }
    }

    /**
     * Gibt eine Error-Message aus,
     * wenn die Nachricht nicht gesendet werden kann
     *
     * @param message Message des Clients
     */
    private void offerMessage(Message message) {
        if (!messages.offer(message)) System.err.println("Message could not have been send");
    }
}
