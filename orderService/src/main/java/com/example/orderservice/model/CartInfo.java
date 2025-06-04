package com.example.orderservice.model;

import java.util.ArrayList;
import java.util.List;

public class CartInfo {
    // orderNum có thể được sinh bởi service, không cần thiết trong request DTO này
    // private int orderNum;
    private CustomerInfo customerInfo;
    private List<CartLineInfo> cartLines = new ArrayList<>();

    public CartInfo() {}

    public CustomerInfo getCustomerInfo() { return customerInfo; }
    public void setCustomerInfo(CustomerInfo customerInfo) { this.customerInfo = customerInfo; }
    public List<CartLineInfo> getCartLines() { return this.cartLines; }
    public void setCartLines(List<CartLineInfo> cartLines) { this.cartLines.clear(); if(cartLines!=null) this.cartLines.addAll(cartLines); }

    public boolean isEmpty() { return this.cartLines.isEmpty(); }
    public boolean isValidCustomer() { return this.customerInfo != null; } // Logic valid có thể phức tạp hơn
    public double getAmountTotal() {
        double total = 0;
        for (CartLineInfo line : this.cartLines) {
            total += line.getAmount();
        }
        return total;
    }
}