package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.BadRequestException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.product.ProductApplication;
import com.yas.product.model.Brand;
import com.yas.product.model.Category;
import com.yas.product.model.Product;
import com.yas.product.repository.BrandRepository;
import com.yas.product.repository.CategoryRepository;
import com.yas.product.repository.ProductCategoryRepository;
import com.yas.product.repository.ProductImageRepository;
import com.yas.product.repository.ProductOptionCombinationRepository;
import com.yas.product.repository.ProductOptionRepository;
import com.yas.product.repository.ProductOptionValueRepository;
import com.yas.product.repository.ProductRelatedRepository;
import com.yas.product.repository.ProductRepository;
import com.yas.product.viewmodel.product.ProductGetDetailVm;
import com.yas.product.viewmodel.product.ProductListVm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = ProductApplication.class)
class ProductServiceTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private ProductOptionValueRepository productOptionValueRepository;

    @Autowired
    private ProductOptionCombinationRepository productOptionCombinationRepository;

    @Autowired
    private ProductRelatedRepository productRelatedRepository;

    @MockitoBean
    private MediaService mediaService;

    @Autowired
    private ProductService productService;

    private Brand testBrand;
    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testBrand = new Brand();
        testBrand.setName("Test Brand");
        testBrand.setSlug("test-brand");
        testBrand.setIsPublished(true);
        testBrand = brandRepository.save(testBrand);

        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setSlug("test-category");
        testCategory.setDescription("Test Description");
        testCategory.setMetaKeyword("test");
        testCategory.setMetaDescription("Test Meta");
        testCategory.setDisplayOrder((short) 1);
        testCategory.setIsPublished(true);
        testCategory = categoryRepository.save(testCategory);

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setSlug("test-product");
        testProduct.setDescription("Test Description");
        testProduct.setShortDescription("Short description");
        testProduct.setSku("SKU123");
        testProduct.setGtin("GTIN123");
        testProduct.setPrice(100L);
        testProduct.setIsPublished(true);
        testProduct.setBrand(testBrand);
        testProduct.setLength(10);
        testProduct.setWidth(5);
        testProduct.setHeight(8);
        testProduct.setWeight(1000L);
    }

    @AfterEach
    void tearDown() {
        productOptionCombinationRepository.deleteAll();
        productOptionValueRepository.deleteAll();
        productOptionRepository.deleteAll();
        productImageRepository.deleteAll();
        productCategoryRepository.deleteAll();
        productRelatedRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
    }

    @Nested
    class GetProductByIdTest {
        @Test
        void getProductById_whenProductExists_shouldReturnProduct() {
            testProduct = productRepository.save(testProduct);

            ProductGetDetailVm result = productService.getProductById(testProduct.getId());

            assertNotNull(result);
            assertEquals(testProduct.getName(), result.name());
            assertEquals(testProduct.getId(), result.id());
        }

        @Test
        void getProductById_whenProductNotFound_shouldThrowNotFoundException() {
            Long nonExistentId = 99999L;

            assertThrows(NotFoundException.class, () -> productService.getProductById(nonExistentId));
        }
    }

    @Nested
    class GetProductBySlugTest {
        @Test
        void getProductBySlug_whenProductExists_shouldReturnProduct() {
            testProduct = productRepository.save(testProduct);

            ProductGetDetailVm result = productService.getProductBySlug(testProduct.getSlug());

            assertNotNull(result);
            assertEquals(testProduct.getName(), result.name());
            assertEquals(testProduct.getSlug(), result.slug());
        }

        @Test
        void getProductBySlug_whenProductNotFound_shouldThrowNotFoundException() {
            String nonExistentSlug = "non-existent-product";

            assertThrows(NotFoundException.class, () -> productService.getProductBySlug(nonExistentSlug));
        }
    }

    @Nested
    class GetAllProductsTest {
        @Test
        void getAllProducts_whenProductsExist_shouldReturnProductList() {
            productRepository.save(testProduct);

            ProductListVm result = productService.getAllProducts(0, 10);

            assertNotNull(result);
            assertEquals(1, result.productContent().size());
        }

        @Test
        void getAllProducts_whenNoProductsExist_shouldReturnEmptyList() {
            ProductListVm result = productService.getAllProducts(0, 10);

            assertNotNull(result);
            assertEquals(0, result.productContent().size());
        }
    }

    @Nested
    class CheckProductExistsTest {
        @Test
        void existsById_whenProductExists_shouldReturnTrue() {
            testProduct = productRepository.save(testProduct);

            boolean result = productService.existsById(testProduct.getId());

            assertEquals(true, result);
        }

        @Test
        void existsById_whenProductNotFound_shouldReturnFalse() {
            boolean result = productService.existsById(99999L);

            assertEquals(false, result);
        }
    }

    @Nested
    class GetProductSlugTest {
        @Test
        void getProductSlug_whenProductExists_shouldReturnSlug() {
            testProduct = productRepository.save(testProduct);

            String result = productService.getProductSlug(testProduct.getId());

            assertEquals(testProduct.getSlug(), result);
        }

        @Test
        void getProductSlug_whenProductNotFound_shouldThrowNotFoundException() {
            assertThrows(NotFoundException.class, () -> productService.getProductSlug(99999L));
        }
    }
}
