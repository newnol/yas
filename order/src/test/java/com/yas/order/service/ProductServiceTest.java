package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yas.order.OrderApplication;
import com.yas.order.config.ServiceUrlConfig;
import com.yas.order.viewmodel.product.ProductVariationVm;
import java.util.List;
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

    @BeforeEach
    void setUp() {
        testVariation = new ProductVariationVm(
                1L,
                "Variation 1",
                "SKU001"
        );
    }

    @Nested
    class ProductServiceBasicTest {
        @Test
        void productService_shouldBeAutowired() {
            assertNotNull(productService);
        }

        @Test
        void productVariationVm_shouldBeCreatedCorrectly() {
            assertNotNull(testVariation);
            assertEquals(1L, testVariation.id());
            assertEquals("Variation 1", testVariation.name());
            assertEquals("SKU001", testVariation.sku());
        }

        @Test
        void productServiceDependencies_shouldBeInjected() {
            assertNotNull(restClient);
            assertNotNull(serviceUrlConfig);
            assertNotNull(productService);
        }

        @Test
        void multipleProductVariations_shouldBeCreated() {
            ProductVariationVm variation1 = new ProductVariationVm(1L, "Product A", "SKU001");
            ProductVariationVm variation2 = new ProductVariationVm(2L, "Product B", "SKU002");
            ProductVariationVm variation3 = new ProductVariationVm(3L, "Product C", "SKU003");

            List<ProductVariationVm> variations = List.of(variation1, variation2, variation3);

            assertNotNull(variations);
            assertEquals(3, variations.size());
            assertEquals("Product A", variations.get(0).name());
            assertEquals("Product B", variations.get(1).name());
            assertEquals("Product C", variations.get(2).name());
        }
    }
}
