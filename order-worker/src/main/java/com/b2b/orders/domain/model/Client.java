package com.b2b.orders.domain.model;

import java.util.Objects;

public record Client(
        String clientId,
        String name,
        ClientSegment segment,
        TaxRegime taxRegime,
        String region,
        String channel
) {
    public Client {
        requireText(clientId, "clientId");
        requireText(name, "name");
        Objects.requireNonNull(segment, "segment is required");
        Objects.requireNonNull(taxRegime, "taxRegime is required");
        requireText(region, "region");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
