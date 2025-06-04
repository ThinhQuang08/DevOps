package com.example.orderservice.entity;

import java.io.Serializable;
import javax.persistence.*; // Đảm bảo dùng javax nếu Spring Boot 2.2.5

@Entity
@Table(name = "Order_details")
public class OrderDetail implements Serializable {

    private static final long serialVersionUID = 7550745928843183535L;

    @Id
    @Column(name = "ID", length = 50, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false, foreignKey = @ForeignKey(name = "ORDER_DETAIL_ORD_FK"))
    private Order order;

    @Column(name = "PRODUCT_ID", length = 20, nullable = false)
    private String productId;

    @Column(name = "Product_Name_Snapshot", length = 255)
    private String productNameSnapshot;

    @Column(name = "Quanity", nullable = false) // Hoặc "Quantity"
    private int quanity;

    @Column(name = "Price", nullable = false)
    private double price;

    @Column(name = "Amount", nullable = false)
    private double amount;

    public OrderDetail() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public int getQuanity() { return quanity; }
    public void setQuanity(int quanity) { this.quanity = quanity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}