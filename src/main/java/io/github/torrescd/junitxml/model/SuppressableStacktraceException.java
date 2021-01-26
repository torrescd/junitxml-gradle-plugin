package io.github.torrescd.junitxml.model;

public class SuppressableStacktraceException extends Exception {


    public SuppressableStacktraceException(String message) {
        super(message, null, true, false);
    }

    @Override
    public String toString() {
        return getLocalizedMessage();

    }
}