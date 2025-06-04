package com.example.orderservice.controller;

import com.example.orderservice.model.CartInfo; // DTO để nhận request tạo đơn hàng
import com.example.orderservice.model.OrderDetailInfo;
import com.example.orderservice.model.OrderInfo;
import com.example.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders") // Base path cho API đơn hàng
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    // POST /api/v1/orders - Tạo đơn hàng mới
    // Nhận CartInfo (hoặc một DTO CreateOrderRequest được thiết kế riêng) trong request body
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CartInfo cartInfo) {
        // @RequestBody nói Spring Boot lấy dữ liệu từ body của request HTTP
        // và chuyển đổi nó thành đối tượng CartInfo (sử dụng Jackson cho JSON)
        try {
            if (cartInfo == null || cartInfo.getCustomerInfo() == null || cartInfo.getCartLines().isEmpty()) {
                return ResponseEntity.badRequest().body("Cart information is incomplete or empty.");
            }

            OrderInfo createdOrder = orderService.createOrder(cartInfo);
            logger.info("Order created successfully with ID: {}", createdOrder.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create order due to bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while creating order", e);
            // Không nên trả về chi tiết exception cho client trong production
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while creating the order.");
        }
    }

    // GET /api/v1/orders/{orderId} - Lấy thông tin chi tiết một đơn hàng
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderInfo> getOrderById(@PathVariable String orderId) {
        Optional<OrderInfo> orderInfoOpt = orderService.getOrderInfoByOrderId(orderId);
        if (orderInfoOpt.isPresent()) {
            return ResponseEntity.ok(orderInfoOpt.get());
        } else {
            logger.warn("Order not found with ID: {}", orderId);
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/v1/orders/{orderId}/details - Lấy danh sách chi tiết các sản phẩm trong một đơn hàng
    @GetMapping("/{orderId}/details")
    public ResponseEntity<List<OrderDetailInfo>> getOrderDetailsByOrderId(@PathVariable String orderId) {
        // Kiểm tra xem order có tồn tại không trước khi lấy details (tùy chọn)
        Optional<OrderInfo> orderCheck = orderService.getOrderInfoByOrderId(orderId);
        if (!orderCheck.isPresent()) {
            logger.warn("Attempted to get details for non-existent order ID: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        List<OrderDetailInfo> details = orderService.getOrderDetailInfosByOrderId(orderId);
        if (details.isEmpty() && orderCheck.isPresent()) {
            // Order tồn tại nhưng không có detail items (trường hợp lạ, nhưng có thể xảy ra)
            // Hoặc có thể trả về list rỗng với status 200 OK
            logger.info("No details found for order ID: {}", orderId);
            return ResponseEntity.ok(details); // Trả về list rỗng
        }
        return ResponseEntity.ok(details);
    }
}