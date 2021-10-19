package com.devatom.server;

import java.io.IOException;

public class InvalidCommandExecutionException extends IOException {
    public InvalidCommandExecutionException(String message) {
        super(message);
    }
}
