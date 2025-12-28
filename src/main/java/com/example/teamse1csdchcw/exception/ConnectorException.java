package com.example.teamse1csdchcw.exception;

/**
 * Exception thrown when a source connector encounters an error.
 */
public class ConnectorException extends Exception {

    public ConnectorException(String message) {
        super(message);
    }

    public ConnectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectorException(Throwable cause) {
        super(cause);
    }
}
