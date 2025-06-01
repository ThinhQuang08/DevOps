package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.form.ProductForm;
import com.example.productservice.model.ProductInfo;
import com.example.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

@Service // Đánh dấu đây là một Spring service bean
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true) // Giao dịch chỉ đọc, tối ưu hơn
    public Page<ProductInfo> getAllProducts(int page, int size, String nameFilter) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createDate").descending());
        Page<Product> productPage;
        if (nameFilter != null && !nameFilter.isEmpty()) {
            productPage = productRepository.findByNameContainingIgnoreCase(nameFilter, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }
        return productPage.map(this::convertToProductInfo); // Chuyển đổi Page<Product> thành Page<ProductInfo>
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductInfo> getProductByCode(String code) {
        return productRepository.findById(code).map(this::convertToProductInfo);
    }

    @Override
    @Transactional // Giao dịch có ghi (tạo/cập nhật)
    public ProductInfo saveProduct(ProductForm productForm) throws IOException {
        Product product;
        boolean isNew = true;

        // Kiểm tra xem có phải là cập nhật hay tạo mới
        if (productForm.getCode() != null && !productForm.getCode().isEmpty()) {
            Optional<Product> existingProductOpt = productRepository.findById(productForm.getCode());
            if (existingProductOpt.isPresent()) {
                product = existingProductOpt.get();
                isNew = false;
            } else {
                // Nếu client cung cấp code nhưng không tìm thấy -> tạo mới với code đó
                product = new Product();
                product.setCode(productForm.getCode());
            }
        } else {
            // Tạo mới hoàn toàn, cần cơ chế sinh code nếu không muốn client tự đặt
            // Ví dụ: product.setCode(generateUniqueProductCode());
            // Hiện tại, nếu code không được cung cấp và là tạo mới, code sẽ là null -> DB có thể lỗi nếu code là PK và NOT NULL
            // Cần đảm bảo 'code' được set trước khi lưu nếu nó là @Id
            // Trong Entity Product.java, 'code' là @Id và nullable = false.
            // Vậy nên, nếu là isNew và productForm.getCode() rỗng, CẦN TẠO CODE MỚI HOẶC BÁO LỖI
            if (productForm.getCode() == null || productForm.getCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Product code cannot be empty for a new product if not auto-generated.");
            }
            product = new Product();
            product.setCode(productForm.getCode().trim()); // Lấy code từ form
        }

        if (isNew) {
            product.setCreateDate(new Date());
        }
        product.setName(productForm.getName());
        product.setPrice(productForm.getPrice());

        MultipartFile fileData = productForm.getFileData();
        if (fileData != null && !fileData.isEmpty()) {
            product.setImage(fileData.getBytes());
        } else if (isNew) {
            // Nếu tạo mới mà không có ảnh, có thể set image là null hoặc một ảnh mặc định
            product.setImage(null);
        }
        // Nếu là update mà không có fileData mới, thì giữ nguyên ảnh cũ (không làm gì cả với product.setImage())

        Product savedProduct = productRepository.save(product);
        return convertToProductInfo(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(String code) {
        if (!productRepository.existsById(code)) {
            // Có thể ném ra một exception tùy chỉnh ví dụ ProductNotFoundException
            throw new RuntimeException("Product not found with code: " + code);
        }
        productRepository.deleteById(code);
    }

    // Phương thức tiện ích để chuyển đổi Entity sang DTO
    private ProductInfo convertToProductInfo(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductInfo(product.getCode(), product.getName(), product.getPrice());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> getProductImageBytesByCode(String code) {
        Optional<Product> productOpt = productRepository.findById(code);
        if (productOpt.isPresent() && productOpt.get().getImage() != null) {
            return Optional.of(productOpt.get().getImage());
        }
        return Optional.empty();
    }

    
}