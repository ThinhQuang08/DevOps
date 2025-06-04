package com.example.demo.controller;

import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.exception.ExceptionUtils; // Có thể không cần nữa nếu bắt lỗi API cụ thể
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource; // Cho MultipartFile
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // Có thể không cần ở đây nữa
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap; // Cho MultipartFile
import org.springframework.util.MultiValueMap; // Cho MultipartFile
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.dao.OrderDAO;
import com.example.demo.form.ProductForm; // ProductForm này là của BE, cần đảm bảo khớp với DTO mà API product-service nhận
import com.example.demo.model.OrderDetailInfo;
import com.example.demo.model.OrderInfo;
import com.example.demo.model.ProductInfo; // ProductInfo này là của BE, để nhận response từ API
import com.example.demo.pagination.PaginationResult;
import com.example.demo.validator.TestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
// @Transactional // Có thể không cần thiết ở cấp controller nữa
public class AdminController {

   private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

   @Autowired
   private OrderDAO orderDAO;

   @Autowired
   private RestTemplate restTemplate;

   @Value("${PRODUCT_SERVICE_URL:http://localhost:8081/api/v1/products}")
   private String productServiceBaseUrl;

   @Autowired
   private TestValidator testValidator; // Validator này vẫn dùng cho ProductForm của BE

   @InitBinder
   public void myInitBinder(WebDataBinder dataBinder) {
      Object target = dataBinder.getTarget();
      if (target == null) {
         return;
      }
      System.out.println("Target=" + target);
      if (target.getClass() == ProductForm.class) {
         dataBinder.setValidator(testValidator);
      }
   }

   @RequestMapping(value = { "/admin/login" }, method = RequestMethod.GET)
   public String login(Model model) {
      return "login";
   }

   @RequestMapping(value = { "/admin/accountInfo" }, method = RequestMethod.GET)
   public String accountInfo(Model model) {
      UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      // System.out.println(userDetails.getPassword());
      // System.out.println(userDetails.getUsername());
      // System.out.println(userDetails.isEnabled());
      model.addAttribute("userDetails", userDetails);
      return "accountInfo";
   }

   @RequestMapping(value = { "/admin/orderList" }, method = RequestMethod.GET)
   public String orderList(Model model,
         @RequestParam(value = "page", defaultValue = "1") String pageStr) {
      int page = 1;
      try {
         page = Integer.parseInt(pageStr);
      } catch (Exception e) {
      }
      final int MAX_RESULT = 5;
      final int MAX_NAVIGATION_PAGE = 10;
      PaginationResult<OrderInfo> paginationResult = orderDAO.listOrderInfo(page, MAX_RESULT, MAX_NAVIGATION_PAGE);
      model.addAttribute("paginationResult", paginationResult);
      return "orderList";
   }

   // GET: Show product form (create new or edit existing)
   @RequestMapping(value = { "/admin/product" }, method = RequestMethod.GET)
   public String product(Model model, @RequestParam(value = "code", defaultValue = "") String code) {
      ProductForm productForm = new ProductForm(); // Luôn tạo form mới
      productForm.setNewProduct(true); // Mặc định là tạo mới

      if (code != null && !code.isEmpty()) {
         try {
            String url = productServiceBaseUrl + "/" + code;
            ResponseEntity<ProductInfo> response = restTemplate.getForEntity(url, ProductInfo.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
               ProductInfo productInfo = response.getBody();
               // Chuyển ProductInfo (từ API) sang ProductForm (của BE)
               productForm.setCode(productInfo.getCode());
               productForm.setName(productInfo.getName());
               productForm.setPrice(productInfo.getPrice());
               // productForm.setFileData(null); // Không lấy file data khi edit, chỉ hiển thị thông tin
               productForm.setNewProduct(false); // Đánh dấu là edit
            } else {
               logger.warn("Product not found from API with code: {} - Status: {}", code, response.getStatusCode());
               model.addAttribute("errorMessage", "Product not found with code: " + code);
               // Vẫn trả về form trống để tạo mới nếu không tìm thấy
            }
         } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product not found from API with code: {}. API returned 404.", code);
            model.addAttribute("errorMessage", "Product not found with code: " + code);
         } catch (Exception e) {
            logger.error("Error fetching product for edit with code: {}", code, e);
            model.addAttribute("errorMessage", "Error fetching product: " + e.getMessage());
         }
      }
      model.addAttribute("productForm", productForm);
      return "product"; // Trả về view product.html
   }

   // POST: Save product (create or update)
   @RequestMapping(value = { "/admin/product" }, method = RequestMethod.POST)
   public String productSave(Model model,
         @ModelAttribute("productForm") @Validated ProductForm productForm,
         BindingResult result,
         final RedirectAttributes redirectAttributes) {

      if (result.hasErrors()) {
         // Nếu có lỗi validation từ ProductFormValidator, giữ lại productForm để hiển thị lỗi
         return "product";
      }

      try {
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.MULTIPART_FORM_DATA);

         MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
         // Các trường của ProductForm mà API product-service cần
         if (productForm.getCode() != null && !productForm.getCode().trim().isEmpty()) {
             body.add("code", productForm.getCode().trim());
         }
         body.add("name", productForm.getName());
         body.add("price", String.valueOf(productForm.getPrice())); // API có thể nhận String rồi parse

         if (productForm.getFileData() != null && !productForm.getFileData().isEmpty()) {
            ByteArrayResource fileResource = new ByteArrayResource(productForm.getFileData().getBytes()) {
               @Override
               public String getFilename() {
                  // Trả về tên file gốc để server API có thể xử lý đúng phần mở rộng
                  return productForm.getFileData().getOriginalFilename();
               }
            };
            body.add("fileData", fileResource);
         }
         // Trường 'newProduct' không cần gửi, API product-service sẽ tự xác định dựa trên sự tồn tại của 'code'

         HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
         ResponseEntity<ProductInfo> response; // ProductInfo là DTO trả về từ API product-service

         if (productForm.isNewProduct() || productForm.getCode() == null || productForm.getCode().trim().isEmpty()) {
            // Tạo mới (POST)
            // Giả sử API tạo mới của product-service không yêu cầu 'code' trong URL, chỉ trong body nếu có
            response = restTemplate.postForEntity(productServiceBaseUrl, requestEntity, ProductInfo.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
               redirectAttributes.addFlashAttribute("message", "Product created successfully!");
            } else {
               // Xử lý lỗi từ API
               model.addAttribute("productForm", productForm); // Giữ lại form data
               model.addAttribute("errorMessage", "Error creating product via API: " + response.getStatusCode());
               logger.error("Error creating product via API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
               return "product";
            }
         } else {
            // Cập nhật (PUT)
            String updateUrl = productServiceBaseUrl + "/" + productForm.getCode();
            response = restTemplate.exchange(updateUrl, HttpMethod.PUT, requestEntity, ProductInfo.class);
             if (response.getStatusCode() == HttpStatus.OK) {
               redirectAttributes.addFlashAttribute("message", "Product updated successfully!");
            } else {
               model.addAttribute("productForm", productForm);
               model.addAttribute("errorMessage", "Error updating product via API: " + response.getStatusCode());
               logger.error("Error updating product via API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
               return "product";
            }
         }
      } catch (HttpClientErrorException e) {
         logger.error("API Client Error during product save. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
         model.addAttribute("productForm", productForm);
         model.addAttribute("errorMessage", "API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
         return "product";
      } catch (IOException ioe) { // Từ getBytes() của MultipartFile
         logger.error("Error processing uploaded file.", ioe);
         model.addAttribute("productForm", productForm);
         model.addAttribute("errorMessage", "Error processing uploaded file: " + ioe.getMessage());
         return "product";
      } catch (Exception e) {
         logger.error("Unexpected error during product save.", e);
         model.addAttribute("productForm", productForm);
         model.addAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
         return "product";
      }

      return "redirect:/productList"; // Hoặc trang admin quản lý sản phẩm
   }

   @RequestMapping(value = { "/admin/order" }, method = RequestMethod.GET)
   public String orderView(Model model, @RequestParam("orderId") String orderId) {
      OrderInfo orderInfo = null;
      if (orderId != null) {
         orderInfo = this.orderDAO.getOrderInfo(orderId);
      }
      if (orderInfo == null) {
         return "redirect:/admin/orderList";
      }
      List<OrderDetailInfo> details = this.orderDAO.listOrderDetailInfos(orderId);
      orderInfo.setDetails(details);
      model.addAttribute("orderInfo", orderInfo);
      return "order";
   }
}