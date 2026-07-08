package com.b2b.orders.infrastructure.messaging.kafka;

public class InvalidOrderMessageException extends RuntimeException {

    public InvalidOrderMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
