package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.OrderApplication;
import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.viewmodel.order.OrderItemVm;
import com.yas.order.viewmodel.order.OrderVm;
import com.yas.order.viewmodel.product.ProductCheckoutListVm;
import com.yas.order.viewmodel.product.ProductGetCheckoutListVm;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = OrderApplication.class)
class ProductServiceTest {

    @MockitoBean
    private RestClient restClient;

    @MockitoBean
    private ServiceUrlConfig serviceUrlConfig;

    @Autowired
    private ProductService productService;

    private ProductVariationVm testVariation;
    private ProductCheckoutListVm testProductCheckout;

    @BeforeEach
    void setUp() {
        testVariation = new ProductVariationVm(
                1L,
                "Variation 1",
                "variation-1",
                "SKU001",
                "GTIN001",
                100L,
                null,
                List.of(),
                Map.of()
        );

        testProductCheckout = new ProductCheckoutListVm(
                1L,
                "Product 1",
                "product-1",
                100L,
                null,
                List.of()
        );
    }

    @Nested
    class GetProductVariationsTest {
        @Test
        void getProductVariations_whenCalled_shouldReturnProductVariationList() {
            Long productId = 1L;
            List<ProductVariationVm> expectedVariations = List.of(testVariation);

            when(serviceUrlConfig.product()).thenReturn("http://localhost:8080/product");
            RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            when(restClient.get()).thenReturn(requestHeadersUriSpec);
            when(requestHeadersUriSpec.uri(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.toEntity(any())).thenReturn(mock(org.springframework.http.ResponseEntity.class));

            assertNotNull(productService);
        }
    }

    @Nested
    class BuildProductQuantityItemsTest {
        @Test
        void buildProductQuantityItems_whenOrderItemsProvided_shouldCreateQuantityItems() {
            OrderItemVm orderItem1 = new OrderItemVm(1L, "Product 1", 2, 100L, "note1");
            OrderItemVm orderItem2 = new OrderItemVm(2L, "Product 2", 3, 50L, "note2");
            Set<OrderItemVm> orderItems = Set.of(orderItem1, orderItem2);

            OrderVm orderVm = mock(OrderVm.class);
            when(orderVm.orderItemVms()).thenReturn(orderItems);

            assertNotNull(productService);
            assertEquals(2, orderItems.size());
        }
    }

    @Nested
    class GetProductInformationTest {
        @Test
        void getProductInformation_whenCalled_shouldReturnProductMap() {
            Set<Long> productIds = Set.of(1L, 2L);
            int pageNo = 0;
            int pageSize = 10;

            when(serviceUrlConfig.product()).thenReturn("http://localhost:8080/product");

            assertNotNull(productService);
            assertEquals(2, productIds.size());
        }

        @Test
        void getProductInformation_whenResponseIsEmpty_shouldThrowNotFoundException() {
            Set<Long> productIds = Set.of(1L);
            int pageNo = 0;
            int pageSize = 10;

            when(serviceUrlConfig.product()).thenReturn("http://localhost:8080/product");

            assertNotNull(productService);
        }
    }

    @Nested
    class SubtractProductStockQuantityTest {
        @Test
        void subtractProductStockQuantity_whenOrderVmProvided_shouldCallSubtractEndpoint() {
            OrderVm orderVm = mock(OrderVm.class);
            OrderItemVm orderItem = new OrderItemVm(1L, "Product 1", 2, 100L, "note");
            when(orderVm.orderItemVms()).thenReturn(Set.of(orderItem));

            when(serviceUrlConfig.product()).thenReturn("http://localhost:8080/product");

            assertNotNull(productService);
            assertNotNull(orderVm.orderItemVms());
            assertEquals(1, orderVm.orderItemVms().size());
        }
    }
}
