package com.b2b.orders.infrastructure.http.dto;

import com.b2b.orders.domain.model.Client;
import com.b2b.orders.domain.model.ClientSegment;
import com.b2b.orders.domain.model.TaxRegime;

public record ClientResponse(
        String clientId,
        String name,
        ClientSegment segment,
        TaxRegime taxRegime,
        String region,
        String channel
) {
    public Client toDomain() {
        return new Client(clientId, name, segment, taxRegime, region, channel);
    }
}
