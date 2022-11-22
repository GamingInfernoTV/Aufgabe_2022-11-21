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

    public Message(Action action, String user) {
        this(action, user, switch (action) {
            case JOIN -> "User '" + user + "' joined";
            case LEAVE -> "User '" + user + "' left";
            default -> throw new IllegalArgumentException(
                    "Message content argument may only be omitted on JOIN or LEAVE action");
        });
    }

    public enum Action {
        JOIN, SEND, LEAVE; // Anmelden, Nachricht senden, Abmelden

        public static Action getFromString(String actionStr) {
            return switch (actionStr) {
                case "JOIN" -> JOIN;
                case "SEND" -> SEND;
                case "LEAVE" -> LEAVE;
                default -> throw new IllegalArgumentException("string cannot be parsed into an action object");
            };
        }
    }

    public static Message getFromString(String msg) {
        if (String.valueOf(msg).matches(
                "^\\{\"action\":\"[^\"]+\",\"user\":\"[^\"]+\",\"message\":\"[^\"]+\"}$")) {
            String[] msgParts = msg.split(
                    "(^\\{\"action\":\")|(\",\"(user|message)\":\")|(\"}$)");
            return new Message(
                    // first element in array is empty because of split, thus start with the second
                    Action.getFromString(msgParts[1]),
                    msgParts[2],
                    msgParts[3]);
        } else throw new IllegalArgumentException("string cannot pe parsed into a message object");
    }

    @Override
    public String toString() {
        return String.format(
                "{\"action\":\"%s\",\"user\":\"%s\",\"message\":\"%s\"}",
                action,
                user,
                content);
    }
}