package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderDetail;
import com.example.orderservice.model.CartInfo;
import com.example.orderservice.model.CartLineInfo;
import com.example.orderservice.model.CustomerInfo;
import com.example.orderservice.model.OrderDetailInfo;
import com.example.orderservice.model.OrderInfo;
import com.example.orderservice.model.ProductInfo;
import com.example.orderservice.repository.OrderDetailRepository;
import com.example.orderservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    private int getMaxOrderNum() {
        // Tạm thời dùng cách đơn giản, cần cải thiện cho production
        List<Order> allOrders = orderRepository.findAll();
        return allOrders.stream().mapToInt(Order::getOrderNum).max().orElse(0);
    }

    @Override
    @Transactional
    public OrderInfo createOrder(CartInfo cartInfo) {
        if (cartInfo == null || cartInfo.getCustomerInfo() == null || cartInfo.getCartLines().isEmpty()) {
            throw new IllegalArgumentException("Cart information is invalid or empty.");
        }

        Order order = new Order();
        int orderNum = this.getMaxOrderNum() + 1;

        order.setId(UUID.randomUUID().toString());
        order.setOrderNum(orderNum);
        order.setOrderDate(new Date());
        order.setAmount(cartInfo.getAmountTotal());

        CustomerInfo customerInfo = cartInfo.getCustomerInfo();
        order.setCustomerName(customerInfo.getName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setCustomerPhone(customerInfo.getPhone());
        order.setCustomerAddress(customerInfo.getAddress());

        Order savedOrder = orderRepository.save(order);

        List<CartLineInfo> lines = cartInfo.getCartLines();
        for (CartLineInfo line : lines) {
            OrderDetail detail = new OrderDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setOrder(savedOrder);
            detail.setAmount(line.getAmount());
            detail.setPrice(line.getProductInfo().getPrice());
            detail.setQuanity(line.getQuantity());

            ProductInfo productInfo = line.getProductInfo();
            detail.setProductId(productInfo.getCode());
            detail.setProductNameSnapshot(productInfo.getName());

            orderDetailRepository.save(detail);
        }
        return convertToOrderInfo(savedOrder, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderInfo> getOrderInfoByOrderId(String orderId) {
        return orderRepository.findById(orderId)
                .map(order -> convertToOrderInfo(order, true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetailInfo> getOrderDetailInfosByOrderId(String orderId) {
        return orderDetailRepository.findByOrderId(orderId).stream()
                .map(this::convertToOrderDetailInfo)
                .collect(Collectors.toList());
    }

    private OrderInfo convertToOrderInfo(Order order, boolean includeDetails) {
        if (order == null) return null;
        OrderInfo orderInfo = new OrderInfo(
                order.getId(), order.getOrderDate(), order.getOrderNum(), order.getAmount(),
                order.getCustomerName(), order.getCustomerAddress(), order.getCustomerEmail(), order.getCustomerPhone()
        );
        if (includeDetails) {
            List<OrderDetailInfo> detailInfos = orderDetailRepository.findByOrderId(order.getId())
                    .stream()
                    .map(this::convertToOrderDetailInfo)
                    .collect(Collectors.toList());
            orderInfo.setDetails(detailInfos);
        }
        return orderInfo;
    }

    private OrderDetailInfo convertToOrderDetailInfo(OrderDetail detail) {
        if (detail == null) return null;
        return new OrderDetailInfo(
                detail.getId(),
                detail.getProductId(),
                detail.getProductNameSnapshot(),
                detail.getQuanity(),
                detail.getPrice(),
                detail.getAmount()
        );
    }
}