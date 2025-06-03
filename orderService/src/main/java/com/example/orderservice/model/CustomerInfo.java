package com.example.orderservice.model;

public class CustomerInfo {
    private String name;
    private String address;
    private String email;
    private String phone;
    private boolean valid; // Có thể không cần trường này trong DTO của order-service

    public CustomerInfo() {}

    // Constructor để dễ tạo từ request nếu cần
    public CustomerInfo(String name, String address, String email, String phone) {
        this.name = name;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.valid = true; // Mặc định là valid khi tạo từ dữ liệu
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isValid() { return valid; } // Có thể bỏ nếu API không dùng
    public void setValid(boolean valid) { this.valid = valid; }
}