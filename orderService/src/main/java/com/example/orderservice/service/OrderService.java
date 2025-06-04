package com.example.orderservice.service;

import com.example.orderservice.model.CartInfo;
import com.example.orderservice.model.OrderDetailInfo;
import com.example.orderservice.model.OrderInfo;
// import org.springframework.data.domain.Page; // Nếu có phân trang đơn hàng

import java.util.List;
import java.util.Optional;

public interface OrderService {
    OrderInfo createOrder(CartInfo cartInfo);
    Optional<OrderInfo> getOrderInfoByOrderId(String orderId);
    List<OrderDetailInfo> getOrderDetailInfosByOrderId(String orderId);
    // Page<OrderInfo> listOrders(int page, int size); // Ví dụ nếu cần
}