package org.renci.gerese4j.core;

public class GeReSe4jException extends Exception {

    private static final long serialVersionUID = -6487717437229588943L;

    public GeReSe4jException() {
        super();
    }

    public GeReSe4jException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public GeReSe4jException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeReSe4jException(String message) {
        super(message);
    }

    public GeReSe4jException(Throwable cause) {
        super(cause);
    }

}
