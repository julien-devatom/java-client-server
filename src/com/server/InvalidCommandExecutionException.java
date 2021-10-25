package com.server;

/**
 * Exception renvoyee lorsque le serveur n'a pas pu executer la commande demandee
 */
public class InvalidCommandExecutionException extends Exception {
    public InvalidCommandExecutionException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}