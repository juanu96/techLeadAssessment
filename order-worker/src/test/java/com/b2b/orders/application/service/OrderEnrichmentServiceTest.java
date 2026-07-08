package com.b2b.orders.application.service;

import com.b2b.orders.application.port.out.ClientCachePort;
import com.b2b.orders.application.port.out.ClientCatalogPort;
import com.b2b.orders.application.port.out.ProductCachePort;
import com.b2b.orders.application.port.out.ProductCatalogPort;
import com.b2b.orders.domain.model.Client;
import com.b2b.orders.domain.model.ClientSegment;
import com.b2b.orders.domain.model.OrderItem;
import com.b2b.orders.domain.model.PendingOrder;
import com.b2b.orders.domain.model.Product;
import com.b2b.orders.domain.model.TaxCategory;
import com.b2b.orders.domain.model.TaxRegime;
import com.b2b.orders.domain.service.TaxCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEnrichmentServiceTest {

    private static final Instant PROCESSED_AT = Instant.parse("2024-09-12T10:45:03.241Z");

    @Mock
    private ProductCatalogPort productCatalog;
    @Mock
    private ClientCatalogPort clientCatalog;
    @Mock
    private ProductCachePort productCache;
    @Mock
    private ClientCachePort clientCache;

    private OrderEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new OrderEnrichmentService(
                productCatalog,
                clientCatalog,
                productCache,
                clientCache,
                new OrderTaxService(new TaxCalculator()),
                "COP",
                Clock.fixed(PROCESSED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void enrichesAnOrderUsingExternalCatalogsAndStoresCacheEntries() {
        Product taxedProduct = product("PRD-001", TaxCategory.GRAVADO);
        Product reducedProduct = product("PRD-008", TaxCategory.REDUCIDO);
        Client client = client();

        when(productCache.findById("PRD-001")).thenReturn(Mono.empty());
        when(productCache.findById("PRD-008")).thenReturn(Mono.empty());
        when(productCatalog.findById("PRD-001")).thenReturn(Mono.just(taxedProduct));
        when(productCatalog.findById("PRD-008")).thenReturn(Mono.just(reducedProduct));
        when(productCache.save(taxedProduct)).thenReturn(Mono.empty());
        when(productCache.save(reducedProduct)).thenReturn(Mono.empty());
        when(clientCache.findById("CLI-99821")).thenReturn(Mono.empty());
        when(clientCatalog.findById("CLI-99821")).thenReturn(Mono.just(client));
        when(clientCache.save(client)).thenReturn(Mono.empty());

        StepVerifier.create(service.enrich(orderWithTwoProducts()))
                .assertNext(result -> {
                    assertThat(result.orderId()).isEqualTo("ORD-2024-COL-00147");
                    assertThat(result.client()).isEqualTo(client);
                    assertThat(result.items()).hasSize(2);
                    assertThat(result.summary().subtotal()).isEqualByComparingTo("182400.00");
                    assertThat(result.summary().totalTax()).isEqualByComparingTo("20880.00");
                    assertThat(result.summary().grandTotal()).isEqualByComparingTo("203280.00");
                    assertThat(result.processedAt()).isEqualTo(PROCESSED_AT);
                })
                .verifyComplete();

        verify(productCache).save(taxedProduct);
        verify(productCache).save(reducedProduct);
        verify(clientCache).save(client);
    }

    @Test
    void usesCachedCatalogDataWithoutCallingExternalApis() {
        Product product = product("PRD-001", TaxCategory.GRAVADO);
        Client client = client();
        PendingOrder order = orderWithDuplicateProduct();

        when(productCache.findById("PRD-001")).thenReturn(Mono.just(product));
        when(clientCache.findById("CLI-99821")).thenReturn(Mono.just(client));

        StepVerifier.create(service.enrich(order))
                .assertNext(result -> assertThat(result.items()).hasSize(2))
                .verifyComplete();

        verify(productCache).findById("PRD-001");
        verify(productCatalog, never()).findById("PRD-001");
        verify(clientCatalog, never()).findById("CLI-99821");
    }

    @Test
    void propagatesCatalogErrorsWithoutBlocking() {
        when(productCache.findById("PRD-001")).thenReturn(Mono.empty());
        when(productCatalog.findById("PRD-001"))
                .thenReturn(Mono.error(new IllegalStateException("products api unavailable")));
        when(clientCache.findById("CLI-99821")).thenReturn(Mono.just(client()));

        StepVerifier.create(service.enrich(orderWithTwoProducts()))
                .expectErrorMessage("products api unavailable")
                .verify();
    }

    private PendingOrder orderWithTwoProducts() {
        return new PendingOrder(
                "ORD-2024-COL-00147",
                "CLI-99821",
                "B2B",
                Instant.parse("2024-09-12T10:45:00Z"),
                List.of(
                        item("PRD-001", "24", "3500.00"),
                        item("PRD-008", "12", "8200.00")
                )
        );
    }

    private PendingOrder orderWithDuplicateProduct() {
        return new PendingOrder(
                "ORD-002",
                "CLI-99821",
                "B2B",
                Instant.parse("2024-09-12T10:45:00Z"),
                List.of(
                        item("PRD-001", "2", "100.00"),
                        item("PRD-001", "1", "100.00")
                )
        );
    }

    private OrderItem item(String productId, String quantity, String unitPrice) {
        return new OrderItem(
                productId,
                new BigDecimal(quantity),
                new BigDecimal(unitPrice)
        );
    }

    private Product product(String productId, TaxCategory taxCategory) {
        return new Product(
                productId,
                "Producto " + productId,
                "SKU-" + productId,
                "Categoría",
                taxCategory,
                "UNIDAD"
        );
    }

    private Client client() {
        return new Client(
                "CLI-99821",
                "Distribuidora Andina S.A.S",
                ClientSegment.MAYORISTA,
                TaxRegime.RESPONSABLE_IVA,
                "Valle del Cauca",
                "B2B"
        );
    }
}
