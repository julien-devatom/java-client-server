package com.devatom.server;

import java.io.IOException;

public class CommandNotFoundException extends IOException {
    public CommandNotFoundException(String cmd, String[] enabledOperations) {
        super(cmd + " must be one of " + String.join(", ", enabledOperations));
    }
}
