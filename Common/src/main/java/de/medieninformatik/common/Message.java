package de.medieninformatik.common;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Serialisierbare Klasse zum Austausch von Nachrichten
 * zwischen Client und Server.
 *
 * @author Externe Quellen
 * @author Malte Kasolowsky <code>m30114</code>
 */
public record Message(Action action, String user, String content) implements Serializable {
    private static final Pattern MATCHER
            = Pattern.compile("^\\{\"action\":\"[^\"]+\",\"user\":\"[^\"]+\",\"message\":\"[^\"]+\"}$");
    private static final Pattern SPLITTER
            = Pattern.compile("(^\\{\"action\":\")|(\",\"(user|message)\":\")|(\"}$)");

    /**
     * Konstruktor; erzeigt eine neue Instant ausgehend von den übergebenen Werten
     *
     * @param action  Die Aktion, welche mit der Message ausgeführt werden soll; darf nicht null sein
     * @param user    Der Urheber der Message; darf nicht null sein
     * @param content Der Inhalt der Message; darf nicht null sein
     * @throws NullPointerException Wenn einer der Werte null ist
     */
    public Message(Action action, String user, String content) {
        this.action = Objects.requireNonNull(action);
        this.user = Objects.requireNonNull(user);
        this.content = Objects.requireNonNull(content);
    }

    /**
     * Konstruktor; erzeugt eine neue Instanz, sofern die Action gleich JOIN oder LEAVE ist,
     * wobei der Content der Message entsprechend initialisiert wird
     *
     * @param action Die Aktion, welche mit der Nachricht ausgeführt wird
     * @param user   Der User, welcher der Urheber der nachricht ist
     * @throws IllegalArgumentException Wenn die übergebene Action nicht JOIN oder LEAVE ist
     */
    public Message(Action action, String user) {
        this(action, user, switch (action) {
            case JOIN -> "User '" + user + "' joined";
            case LEAVE -> "User '" + user + "' left";
            default -> throw new IllegalArgumentException(
                    "Message content argument may only be omitted on JOIN or LEAVE action"
            );
        });
    }

    /**
     * Erzeugt eine neue {@link Message} ausgehend von einem String
     *
     * @return eine neue Instanz, deren Werte aus dem übergebenen String gezogen wurden
     * @throws IllegalArgumentException Wenn der übergebene String nicht in ein neues Objekt umgewandelt werden kann
     */
    public static Message getFromString(String msg) {
        if (MATCHER.matcher(String.valueOf(msg)).matches()) {
            String[] msgParts = SPLITTER.split(msg);
            return new Message(
                    // first element in array is empty because of split, thus start with the second
                    Action.getFromString(msgParts[1]), msgParts[2], msgParts[3]
            );
        } else throw new IllegalArgumentException("string cannot pe parsed into a message object");
    }

    /**
     * Erzeugt einen String aus den Werten der Message; dieser entspricht dem Muster,
     * welches für {@link Message#getFromString(String)} benötigt wird
     *
     * @return Die Message als String
     */
    @Override
    public String toString() {
        return "{\"action\":\"%s\",\"user\":\"%s\",\"message\":\"%s\"}".formatted(action, user, content);
    }

    /**
     * Inneres Enum, welche die Aktionen, die die Nachricht ausführen kann beinhaltet
     */
    public enum Action {
        JOIN, SEND, LEAVE; // Anmelden, Nachricht senden, Abmelden

        /**
         * Unterscheidet welche der möglichen Actions ausgeführt wurde
         *
         * @param actionStr String mit ausgeführter Action
         * @return gibt weiter welcher Fall eingetroffen ist
         */
        public static Action getFromString(String actionStr) {
            return switch (actionStr) {
                case "JOIN" -> JOIN;
                case "SEND" -> SEND;
                case "LEAVE" -> LEAVE;
                default -> throw new IllegalArgumentException("string cannot be parsed into an action object");
            };
        }
    }
}
