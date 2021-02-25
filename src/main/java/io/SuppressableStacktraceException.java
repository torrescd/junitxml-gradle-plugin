package io;

public class SuppressableStacktraceException extends Exception {


    public SuppressableStacktraceException(String message) {
        super(message, null, true, false);
    }

    @Override
    public String toString() {
        return getLocalizedMessage();

    }
}