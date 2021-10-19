package com.client;

import java.io.IOException;

/**
 * La commande a été invalidée coté client. Permet de détecter si il y
 * a une commande connue côté client qui est mal formulée, sans envoyer de données au serveur.
 */
public class InvalidClientExecutionException extends IOException {
    public InvalidClientExecutionException(String message) {
        super(message);
    }
}
