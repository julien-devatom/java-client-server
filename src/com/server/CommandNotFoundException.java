package com.server;

import java.io.IOException;

/**
 * La commande demandee n'existe pas. On affiche donc les commandes disponibles sur le serveur.
 */
public class CommandNotFoundException extends IOException {
    public CommandNotFoundException(String cmd, String[] enabledOperations) {
        super(cmd + " is not a valid command. Try one of " + String.join(", ", enabledOperations));
    }
}