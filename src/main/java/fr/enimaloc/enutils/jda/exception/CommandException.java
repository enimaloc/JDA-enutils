package fr.enimaloc.enutils.jda.exception;

public class CommandException extends RuntimeException {
    public CommandException(Exception e) {
        super(e);
    }
}