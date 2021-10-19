package com.devatom.client;

import java.io.IOException;

public class InvalidClientExecutionException extends IOException {
    public InvalidClientExecutionException(String message) {
        super(message);
    }
}
