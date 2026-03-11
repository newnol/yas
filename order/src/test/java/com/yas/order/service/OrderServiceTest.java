package com.yas.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.OrderApplication;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderBriefVm;
import com.yas.order.viewmodel.order.OrderVm;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = OrderApplication.class)
class OrderServiceTest {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private PromotionService promotionService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    private Order testOrder;
    private OrderAddress testBillingAddress;
    private OrderAddress testShippingAddress;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testBillingAddress = OrderAddress.builder()
                .phone("123456789")
                .contactName("John Doe")
                .addressLine1("123 Main St")
                .addressLine2("Apt 4")
                .city("New York")
                .zipCode("10001")
                .districtId(1L)
                .districtName("Manhattan")
                .stateOrProvinceId(1L)
                .stateOrProvinceName("New York")
                .countryId(1L)
                .countryName("United States")
                .build();

        testShippingAddress = OrderAddress.builder()
                .phone("123456789")
                .contactName("John Doe")
                .addressLine1("456 Oak Ave")
                .addressLine2("")
                .city("Brooklyn")
                .zipCode("11201")
                .districtId(2L)
                .districtName("Brooklyn")
                .stateOrProvinceId(1L)
                .stateOrProvinceName("New York")
                .countryId(1L)
                .countryName("United States")
                .build();

        testOrder = Order.builder()
                .email("test@example.com")
                .note("Test order note")
                .tax(new BigDecimal("10"))
                .discount(new BigDecimal("5"))
                .totalPrice(new BigDecimal("100"))
                .orderStatus(OrderStatus.PENDING)
                .deliveryFee(new BigDecimal("5"))
                .deliveryStatus(DeliveryStatus.PREPARING)
                .paymentStatus(PaymentStatus.PENDING)
                .shippingAddressId(testShippingAddress)
                .billingAddressId(testBillingAddress)
                .checkoutId("checkout123")
                .build();

        testOrderItem = OrderItem.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(2)
                .productPrice(new BigDecimal("50"))
                .note("Test item note")
                .build();
    }

    @AfterEach
    void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Nested
    class GetOrderWithItemsByIdTest {
        @Test
        void getOrderWithItemsById_whenOrderExists_shouldReturnOrder() {
            testOrder = orderRepository.save(testOrder);
            testOrderItem.setOrderId(testOrder.getId());
            orderItemRepository.save(testOrderItem);

            OrderVm result = orderService.getOrderWithItemsById(testOrder.getId());

            assertNotNull(result);
            assertEquals(testOrder.getId(), result.id());
            assertEquals(testOrder.getEmail(), result.email());
        }

        @Test
        void getOrderWithItemsById_whenOrderNotFound_shouldThrowNotFoundException() {
            Long nonExistentId = 99999L;

            assertThrows(NotFoundException.class, () -> orderService.getOrderWithItemsById(nonExistentId));
        }
    }

    @Nested
    class GetLatestOrdersTest {
        @Test
        void getLatestOrders_whenOrdersExist_shouldReturnOrderList() {
            orderRepository.save(testOrder);

            List<OrderBriefVm> result = orderService.getLatestOrders(10);

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        void getLatestOrders_whenNoOrdersExist_shouldReturnEmptyList() {
            List<OrderBriefVm> result = orderService.getLatestOrders(10);

            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        void getLatestOrders_whenCountIsZero_shouldReturnEmptyList() {
            orderRepository.save(testOrder);

            List<OrderBriefVm> result = orderService.getLatestOrders(0);

            assertNotNull(result);
            assertEquals(0, result.size());
        }
    }

    @Nested
    class AcceptOrderTest {
        @Test
        void acceptOrder_whenOrderExists_shouldUpdateOrderStatus() {
            testOrder.setOrderStatus(OrderStatus.PENDING);
            testOrder = orderRepository.save(testOrder);

            orderService.acceptOrder(testOrder.getId());

            Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertEquals(OrderStatus.ACCEPTED, updatedOrder.getOrderStatus());
        }

        @Test
        void acceptOrder_whenOrderNotFound_shouldThrowNotFoundException() {
            Long nonExistentId = 99999L;

            assertThrows(NotFoundException.class, () -> orderService.acceptOrder(nonExistentId));
        }
    }

    @Nested
    class RejectOrderTest {
        @Test
        void rejectOrder_whenOrderExists_shouldUpdateOrderStatus() {
            testOrder.setOrderStatus(OrderStatus.PENDING);
            testOrder = orderRepository.save(testOrder);

            orderService.rejectOrder(testOrder.getId(), "Out of stock");

            Order updatedOrder = orderRepository.findById(testOrder.getId()).orElseThrow();
            assertEquals(OrderStatus.REJECT, updatedOrder.getOrderStatus());
            assertEquals("Out of stock", updatedOrder.getRejectReason());
        }

        @Test
        void rejectOrder_whenOrderNotFound_shouldThrowNotFoundException() {
            Long nonExistentId = 99999L;

            assertThrows(NotFoundException.class,
                    () -> orderService.rejectOrder(nonExistentId, "Test reason"));
        }
    }
}
