package de.medieninformatik.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Serialisierbare Klasse zum Austausch von Nachrichten
 * zwischen Client und Server.
 */
public record Message(Action action, String user, String content) implements Serializable {
    public Message(Action action, String user, String content) {
        this.action = Objects.requireNonNull(action);
        this.user = Objects.requireNonNull(user);
        this.content = Objects.requireNonNull(content);
    }

    /**
     * Setzt die Message zusammen aus den übergebenen Informationen
     *
     * @param action die auf dem Fenster geschieht
     * @param user der aktive Client der auf dem Server angemeldet ist
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
     * Spaltet die Message in ihre Bestandteile auf und entfernt unnötige Formatierungen,
     * sodass überprüft werden kann, ob die Message
     * den Anforderungen des Servers entspricht
     *
     * @param msg übergebene Message des Clients
     * @return gibt die Einzelteile der Message weiter
     */
    public static Message getFromString(String msg) {
        if (String.valueOf(msg).matches(
                "^\\{\"action\":\"[^\"]+\",\"user\":\"[^\"]+\",\"message\":\"[^\"]+\"}$"
        )) {
            String[] msgParts = msg.split("(^\\{\"action\":\")|(\",\"(user|message)\":\")|(\"}$)");
            return new Message(
                    // first element in array is empty because of split, thus start with the second
                    Action.getFromString(msgParts[1]), msgParts[2], msgParts[3]
            );
        } else throw new IllegalArgumentException("string cannot pe parsed into a message object");
    }

    /**
     * Formatiert die Message
     * @return gibt die formatierte Message weiter
     */
    @Override
    public String toString() {
        return "{\"action\":\"%s\",\"user\":\"%s\",\"message\":\"%s\"}".formatted(action, user, content);
    }

    public enum Action {
        JOIN, SEND, LEAVE; // Anmelden, Nachricht senden, Abmelden

        /**
         * Unterscheidet welche der möglichen Actions ausgeführt wurde
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