package com.server;

/**
 * Exception renvoyée lorsque le serveur n'a pas pu executer la commande demandée
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
