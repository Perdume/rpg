package Perdume.rpg.gamemode.raid;

// A custom exception to represent a game-breaking error within a raid.
public class RaidCriticalException extends RuntimeException {

    public RaidCriticalException(String message) {
        super(message);
    }

    public RaidCriticalException(String message, Throwable cause) {
        super(message, cause);
    }
}