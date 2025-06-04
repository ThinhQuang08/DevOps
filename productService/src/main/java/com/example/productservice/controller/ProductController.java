package com.example.productservice.controller;

import com.example.productservice.form.ProductForm; // Điều chỉnh package nếu cần
import com.example.productservice.model.ProductInfo; // Điều chỉnh package nếu cần
import com.example.productservice.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile; // Bỏ comment nếu ProductForm còn dùng MultipartFile và API hỗ trợ

import java.io.IOException; // Nếu ProductService.saveProduct ném IOException
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/products") // Base path cho tất cả các API sản phẩm
public class ProductController {

    @Autowired
    private ProductService productService;

    // GET /api/v1/products - Lấy danh sách sản phẩm (có phân trang và lọc theo tên)
    @GetMapping
    public ResponseEntity<Page<ProductInfo>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        Page<ProductInfo> products = productService.getAllProducts(page, size, name);
        return ResponseEntity.ok(products);
    }

    // GET /api/v1/products/{code} - Lấy chi tiết sản phẩm theo code
    @GetMapping("/{code}")
    public ResponseEntity<ProductInfo> getProductByCode(@PathVariable String code) {
        Optional<ProductInfo> productInfo = productService.getProductByCode(code);
        return productInfo.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /api/v1/products - Tạo sản phẩm mới
    // Hiện tại dùng @ModelAttribute để khớp với ProductForm có MultipartFile.
    // Nếu ProductForm không còn MultipartFile và chỉ là JSON, dùng @RequestBody.
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }) // Hoặc MediaType.APPLICATION_JSON_VALUE nếu không có file
    public ResponseEntity<?> createProduct(@ModelAttribute ProductForm productForm) {
        // Lưu ý: @ModelAttribute sẽ binding từ request parameters hoặc multipart form data.
        // Nếu client gửi JSON thuần, cần dùng @RequestBody ProductForm productForm
        // và ProductForm không nên có trường MultipartFile nếu dùng @RequestBody.
        try {
            ProductInfo savedProduct = productService.saveProduct(productForm);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (IllegalArgumentException e) { // Ví dụ: lỗi validate code, tên...
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) { // Nếu saveProduct ném IOException (từ file upload)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + e.getMessage());
        } catch (Exception e) { // Bắt các lỗi chung khác
            // Nên log lỗi này chi tiết ở server
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    // PUT /api/v1/products/{code} - Cập nhật sản phẩm
    // @PutMapping(value = "/{code}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    // public ResponseEntity<?> updateProduct(@PathVariable String code,
    //                                        @ModelAttribute ProductForm productForm) {
    //     // Đảm bảo code trong path được sử dụng để xác định sản phẩm cần cập nhật
    //     productForm.setCode(code);
    //     try {
    //         ProductInfo updatedProduct = productService.saveProduct(productForm); // saveProduct sẽ xử lý cả tạo mới và cập nhật
    //         if (updatedProduct == null) { // Trường hợp saveProduct trả về null nếu không tìm thấy để cập nhật (logic cũ)
    //             return ResponseEntity.notFound().build();
    //         }
    //         return ResponseEntity.ok(updatedProduct);
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().body(e.getMessage());
    //     } catch (IOException e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + e.getMessage());
    //     } catch (RuntimeException e) { // Ví dụ: ProductNotFoundException từ service.deleteProduct
    //         if (e.getMessage() != null && e.getMessage().contains("not found")) {
    //              return ResponseEntity.notFound().build();
    //         }
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during update.");
    //     }
    // }

   @PutMapping(value = "/{codeFromPath}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> updateProduct(@PathVariable String codeFromPath,
                                        @ModelAttribute ProductForm formWithoutCode) {
        formWithoutCode.setCode(codeFromPath); // Gán code từ path vào form
        
        try {
            ProductInfo updatedProduct = productService.saveProduct(formWithoutCode); // saveProduct sẽ xử lý cả tạo mới và cập nhật
            if (updatedProduct == null) { // Trường hợp saveProduct trả về null nếu không tìm thấy để cập nhật (logic cũ)
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing file: " + e.getMessage());
        } catch (RuntimeException e) { // Ví dụ: ProductNotFoundException từ service.deleteProduct
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                 return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred during update.");
        }      
}

    // DELETE /api/v1/products/{code} - Xóa sản phẩm
    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String code) {
        try {
            productService.deleteProduct(code);
            return ResponseEntity.noContent().build(); // HTTP 204 No Content là chuẩn cho DELETE thành công
        } catch (RuntimeException e) { // Bắt ProductNotFoundException hoặc lỗi tương tự
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{code}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable String code) {
        Optional<byte[]> imageBytesOpt = productService.getProductImageBytesByCode(code);
        return imageBytesOpt
                .map(bytes -> ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes)) // Hoặc IMAGE_PNG
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
