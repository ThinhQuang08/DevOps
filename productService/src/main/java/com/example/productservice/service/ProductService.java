package com.example.productservice.service;

import com.example.productservice.form.ProductForm; // Giả sử vẫn dùng ProductForm cho input API
import com.example.productservice.model.ProductInfo; // Giả sử vẫn dùng ProductInfo cho output API
import org.springframework.data.domain.Page; // Cho kết quả phân trang
import org.springframework.web.multipart.MultipartFile; // Nếu xử lý upload file

import java.io.IOException;
import java.util.Optional;

public interface ProductService {

    Page<ProductInfo> getAllProducts(int page, int size, String nameFilter);

    Optional<ProductInfo> getProductByCode(String code);

    // Trả về ProductInfo của sản phẩm đã lưu (bao gồm code nếu là tạo mới)
    ProductInfo saveProduct(ProductForm productForm) throws IOException; // Thêm IOException nếu xử lý file

    void deleteProduct(String code);

    Optional<byte[]> getProductImageBytesByCode(String code);
}