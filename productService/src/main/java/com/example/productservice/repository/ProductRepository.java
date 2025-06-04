package com.example.productservice.repository;

import com.example.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Annotation này là tùy chọn với Spring Boot cho interface extends JpaRepository
public interface ProductRepository extends JpaRepository<Product, String> {
    // JpaRepository<Product, String> có nghĩa là:
    // - Repository này làm việc với Entity tên là Product.
    // - Kiểu dữ liệu của khóa chính (ID) của Product là String (vì 'code' là String).

    // Spring Data JPA sẽ tự động tạo các phương thức CRUD cơ bản:
    // save(), findById(), findAll(), deleteById(), count(), existsById(), ...

    // Phương thức tùy chỉnh để tìm kiếm sản phẩm theo tên (không phân biệt hoa thường)
    // và hỗ trợ phân trang. Spring Data JPA sẽ tự tạo query từ tên phương thức.
    // Query tương đương: SELECT p FROM Product p WHERE lower(p.name) LIKE lower(concat('%', ?1, '%'))
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Nếu không có tên để lọc, JpaRepository đã có sẵn:
    // Page<Product> findAll(Pageable pageable);
}