package com.benjaminhoogterp;

public class MyResource implements AutoCloseable {
    @Override
    public void close() {
        System.err.println("Resource has been closed.");
    }
}
