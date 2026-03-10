package com.yas.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import com.yas.product.viewmodel.NoFileMediaVm;
import com.yas.product.viewmodel.product.ProductDetailInfoVm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = ProductApplication.class)
class ProductDetailServiceTest {
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
    private ProductDetailService productDetailService;

    private Brand testBrand;
    private Product testProduct;
    private NoFileMediaVm testMediaVm;

    @BeforeEach
    void setUp() {
        testBrand = new Brand();
        testBrand.setName("Test Brand");
        testBrand.setSlug("test-brand");
        testBrand.setIsPublished(true);
        testBrand = brandRepository.save(testBrand);

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setSlug("test-product");
        testProduct.setDescription("Test Description");
        testProduct.setShortDescription("Short description");
        testProduct.setSpecification("Test Specification");
        testProduct.setSku("SKU123");
        testProduct.setGtin("GTIN123");
        testProduct.setPrice(100L);
        testProduct.setIsPublished(true);
        testProduct.setAllowedToOrder(true);
        testProduct.setFeatured(false);
        testProduct.setVisibleIndividually(true);
        testProduct.setStockTrackingEnabled(true);
        testProduct.setBrand(testBrand);
        testProduct.setLength(10);
        testProduct.setWidth(5);
        testProduct.setHeight(8);
        testProduct.setWeight(1000L);
        testProduct.setMetaTitle("Meta Title");
        testProduct.setMetaKeyword("test");
        testProduct.setMetaDescription("Meta Description");

        testMediaVm = new NoFileMediaVm(1L, "caption", "fileName", "mediaType", "http://example.com/image.jpg");
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
    class GetProductDetailByIdTest {
        @Test
        void getProductDetailById_whenPublishedProductExists_shouldReturnProductDetail() {
            testProduct = productRepository.save(testProduct);
            when(mediaService.getMedia(any())).thenReturn(testMediaVm);

            ProductDetailInfoVm result = productDetailService.getProductDetailById(testProduct.getId());

            assertNotNull(result);
            assertEquals(testProduct.getId(), result.id());
            assertEquals(testProduct.getName(), result.name());
            assertEquals(testProduct.getShortDescription(), result.shortDescription());
            assertEquals(testProduct.getDescription(), result.description());
            assertEquals(testProduct.getSku(), result.sku());
            assertEquals(testProduct.getGtin(), result.gtin());
            assertEquals(testProduct.getPrice(), result.price());
        }

        @Test
        void getProductDetailById_whenProductNotPublished_shouldThrowNotFoundException() {
            testProduct.setIsPublished(false);
            testProduct = productRepository.save(testProduct);

            assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(testProduct.getId()));
        }

        @Test
        void getProductDetailById_whenProductNotFound_shouldThrowNotFoundException() {
            Long nonExistentId = 99999L;

            assertThrows(NotFoundException.class, () -> productDetailService.getProductDetailById(nonExistentId));
        }

        @Test
        void getProductDetailById_whenProductHasBrand_shouldReturnBrandInfo() {
            testProduct = productRepository.save(testProduct);
            when(mediaService.getMedia(any())).thenReturn(testMediaVm);

            ProductDetailInfoVm result = productDetailService.getProductDetailById(testProduct.getId());

            assertNotNull(result);
            assertEquals(testBrand.getId(), result.brandId());
            assertEquals(testBrand.getName(), result.brandName());
        }

        @Test
        void getProductDetailById_whenProductHasNoOptions_shouldReturnEmptyVariations() {
            testProduct.setHasOptions(false);
            testProduct = productRepository.save(testProduct);
            when(mediaService.getMedia(any())).thenReturn(testMediaVm);

            ProductDetailInfoVm result = productDetailService.getProductDetailById(testProduct.getId());

            assertNotNull(result);
            assertEquals(0, result.variations().size());
        }

        @Test
        void getProductDetailById_whenProductHasMetadata_shouldReturnMetadata() {
            testProduct = productRepository.save(testProduct);
            when(mediaService.getMedia(any())).thenReturn(testMediaVm);

            ProductDetailInfoVm result = productDetailService.getProductDetailById(testProduct.getId());

            assertNotNull(result);
            assertEquals(testProduct.getMetaTitle(), result.metaTitle());
            assertEquals(testProduct.getMetaKeyword(), result.metaKeyword());
            assertEquals(testProduct.getMetaDescription(), result.metaDescription());
        }
    }
}
