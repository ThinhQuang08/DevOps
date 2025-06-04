package com.example.orderservice.model;

public class CartLineInfo {
    private ProductInfo productInfo;
    private int quantity;

    public CartLineInfo() { this.quantity = 0; }

    public ProductInfo getProductInfo() { return productInfo; }
    public void setProductInfo(ProductInfo productInfo) { this.productInfo = productInfo; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getAmount() {
        if (this.productInfo == null || this.productInfo.getPrice() < 0) return 0;
        return this.productInfo.getPrice() * this.quantity;
    }
}