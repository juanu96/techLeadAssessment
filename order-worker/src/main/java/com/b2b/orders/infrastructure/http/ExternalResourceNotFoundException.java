package com.b2b.orders.infrastructure.http;

public class ExternalResourceNotFoundException extends RuntimeException {

    public ExternalResourceNotFoundException(String message) {
        super(message);
    }
}
