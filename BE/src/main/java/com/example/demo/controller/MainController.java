package com.example.demo.controller;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
// import org.springframework.transaction.annotation.Transactional; // Có thể không cần ở đây nữa
import org.springframework.ui.Model;
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

// import com.example.demo.dao.OrderDAO; // Sẽ không dùng OrderDAO trực tiếp nữa
import com.example.demo.form.CustomerForm;
import com.example.demo.model.CartInfo;
import com.example.demo.model.CustomerInfo;
import com.example.demo.model.ProductInfo;
import com.example.demo.model.OrderInfo; // Import OrderInfo DTO của BE
import com.example.demo.utils.CustomPageImpl;
import com.example.demo.utils.Utils;
import com.example.demo.validator.CustomerFormValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MainController {

   private static final Logger logger = LoggerFactory.getLogger(MainController.class);

   // @Autowired // Không inject OrderDAO nữa
   // private OrderDAO orderDAO;

   @Autowired
   private RestTemplate restTemplate;

   @Value("${PRODUCT_SERVICE_URL:http://localhost:8081/api/v1/products}")
   private String productServiceBaseUrl;

   @Value("${ORDER_SERVICE_URL:http://localhost:8082/api/v1/orders}") // Thêm URL cho Order Service
   private String orderServiceBaseUrl;

   @Autowired
   private CustomerFormValidator customerFormValidator;

   @InitBinder
   public void myInitBinder(WebDataBinder dataBinder) {
      Object target = dataBinder.getTarget();
      if (target == null) { return; }
      if (target.getClass() == CartInfo.class) {
      } else if (target.getClass() == CustomerForm.class) {
         dataBinder.setValidator(customerFormValidator);
      }
   }

   @RequestMapping("/403")
   public String accessDenied() { return "/403"; }

   @RequestMapping("/")
   public String home() { return "index"; }

   @RequestMapping({ "/productList" })
   public String listProductHandler(Model model,
         @RequestParam(value = "name", defaultValue = "") String likeName,
         @RequestParam(value = "page", defaultValue = "0") int page) {
      final int MAX_RESULT = 8;
      String url = productServiceBaseUrl + "?page=" + page + "&size=" + MAX_RESULT;
      if (likeName != null && !likeName.isEmpty()) {
         try {
            url += "&name=" + java.net.URLEncoder.encode(likeName, "UTF-8");
         } catch (java.io.UnsupportedEncodingException e) {
            logger.error("Error encoding likeName parameter", e);
         }
      }
      try {
         ResponseEntity<CustomPageImpl<ProductInfo>> response = restTemplate.exchange(
               url, HttpMethod.GET, null, new ParameterizedTypeReference<CustomPageImpl<ProductInfo>>() {});
         if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            model.addAttribute("paginationProducts", response.getBody());
         } else {
            logger.warn("Could not load products from API. Status: {}, URL: {}", response.getStatusCode(), url);
            model.addAttribute("errorMessage", "Could not load products. Status: " + response.getStatusCode());
            model.addAttribute("paginationProducts", Page.empty());
         }
      } catch (HttpClientErrorException e) {
         logger.error("Error loading products from API. Status: {}, Body: {}, URL: {}", e.getStatusCode(), e.getResponseBodyAsString(), url, e);
         model.addAttribute("errorMessage", "Error loading products: " + e.getStatusCode());
         model.addAttribute("paginationProducts", Page.empty());
      } catch (Exception e) {
         logger.error("Unexpected error loading products from API. URL: {}", url, e);
         model.addAttribute("errorMessage", "Error loading products: " + e.getMessage());
         model.addAttribute("paginationProducts", Page.empty());
      }
      return "productList";
   }

   @RequestMapping({ "/buyProduct" })
   public String buyProductHandler(HttpServletRequest request, Model model,
         @RequestParam(value = "code", defaultValue = "") String code) {
      ProductInfo productInfo = null;
      if (code != null && !code.isEmpty()) {
         try {
            String url = productServiceBaseUrl + "/" + code;
            ResponseEntity<ProductInfo> response = restTemplate.getForEntity(url, ProductInfo.class);
            if (response.getStatusCode() == HttpStatus.OK) {
               productInfo = response.getBody();
            } else {
               logger.warn("Product not found from service with code: {} - Status: {}", code, response.getStatusCode());
            }
         } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Product not found from service with code: {}. API returned 404.", code);
         } catch (Exception e) {
            logger.error("Error fetching product from service with code: {}", code, e);
         }
      }
      if (productInfo != null) {
         CartInfo cartInfo = Utils.getCartInSession(request);
         cartInfo.addProduct(productInfo, 1);
      } else {
         return "redirect:/productList?error=ProductNotFound";
      }
      return "redirect:/shoppingCart";
   }

   @RequestMapping({ "/shoppingCartRemoveProduct" })
   public String removeProductHandler(HttpServletRequest request, @RequestParam(value = "code", defaultValue = "") String code) {
      if (code != null && !code.isEmpty()) {
         CartInfo cartInfo = Utils.getCartInSession(request);
         ProductInfo productInfoToRemove = new ProductInfo();
         productInfoToRemove.setCode(code);
         cartInfo.removeProduct(productInfoToRemove);
      }
      return "redirect:/shoppingCart";
   }

   @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.POST)
   public String shoppingCartUpdateQty(HttpServletRequest request, @ModelAttribute("cartForm") CartInfo cartForm) {
      CartInfo cartInfo = Utils.getCartInSession(request);
      cartInfo.updateQuantity(cartForm);
      return "redirect:/shoppingCart";
   }

   @RequestMapping(value = { "/shoppingCart" }, method = RequestMethod.GET)
   public String shoppingCartHandler(HttpServletRequest request, Model model) {
      CartInfo myCart = Utils.getCartInSession(request);
      model.addAttribute("cartForm", myCart);
      model.addAttribute("myCart", myCart);
      return "shoppingCart";
   }

   @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.GET)
   public String shoppingCartCustomerForm(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);
      if (cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      }
      CustomerInfo customerInfo = cartInfo.getCustomerInfo();
      CustomerForm customerForm = (customerInfo != null) ? new CustomerForm(customerInfo) : new CustomerForm();
      model.addAttribute("customerForm", customerForm);
      return "shoppingCartCustomer";
   }

   @RequestMapping(value = { "/shoppingCartCustomer" }, method = RequestMethod.POST)
   public String shoppingCartCustomerSave(HttpServletRequest request, Model model,
         @ModelAttribute("customerForm") @Validated CustomerForm customerForm,
         BindingResult result, final RedirectAttributes redirectAttributes) {
      if (result.hasErrors()) {
         customerForm.setValid(false);
         return "shoppingCartCustomer";
      }
      customerForm.setValid(true);
      CartInfo cartInfo = Utils.getCartInSession(request);
      CustomerInfo customerInfo = new CustomerInfo(customerForm);
      cartInfo.setCustomerInfo(customerInfo);
      return "redirect:/shoppingCartConfirmation";
   }

   @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.GET)
   public String shoppingCartConfirmationReview(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);
      if (cartInfo == null || cartInfo.isEmpty()) {
         return "redirect:/shoppingCart";
      } else if (!cartInfo.isValidCustomer()) {
         return "redirect:/shoppingCartCustomer";
      }
      model.addAttribute("myCart", cartInfo);
      return "shoppingCartConfirmation";
   }

   // POST: Submit Cart (Save Order via Order Service API)
   @RequestMapping(value = { "/shoppingCartConfirmation" }, method = RequestMethod.POST)
   public String shoppingCartConfirmationSave(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request); // Đây là CartInfo DTO của BE

      if (cartInfo.isEmpty()) {
         logger.warn("Attempted to save an empty cart.");
         return "redirect:/shoppingCart";
      } else if (!cartInfo.isValidCustomer()) {
         logger.warn("Attempted to save cart with invalid customer info.");
         return "redirect:/shoppingCartCustomer";
      }

      try {
         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_JSON);
         HttpEntity<CartInfo> requestEntity = new HttpEntity<>(cartInfo, headers);

         // OrderInfo.class ở đây là com.example.demo.model.OrderInfo (DTO của BE)
         ResponseEntity<OrderInfo> response = restTemplate.postForEntity(
               orderServiceBaseUrl, // URL của order-service API
               requestEntity,
               OrderInfo.class);

         if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
            OrderInfo createdOrder = response.getBody();
            logger.info("Order created successfully via API, Order ID: {}", createdOrder.getId());
            request.getSession().setAttribute("lastOrderInfo", createdOrder);
            Utils.removeCartInSession(request);
            return "redirect:/shoppingCartFinalize";
         } else {
            logger.error("Error creating order via API. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
            model.addAttribute("errorMessage", "Could not create order. Service responded with status: " + response.getStatusCode());
            model.addAttribute("myCart", cartInfo);
            return "shoppingCartConfirmation";
         }
      } catch (HttpClientErrorException e) {
         logger.error("API Client Error while creating order. Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
         model.addAttribute("errorMessage", "API Error: " + e.getStatusCode() + " - Could not save order.");
         model.addAttribute("myCart", cartInfo);
         return "shoppingCartConfirmation";
      } catch (Exception e) {
         logger.error("Unexpected error while saving order via API.", e);
         model.addAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
         model.addAttribute("myCart", cartInfo);
         return "shoppingCartConfirmation";
      }
   }

   @RequestMapping(value = { "/shoppingCartFinalize" }, method = RequestMethod.GET)
   public String shoppingCartFinalize(HttpServletRequest request, Model model) {
      OrderInfo lastOrder = (OrderInfo) request.getSession().getAttribute("lastOrderInfo");
      // Utils.removeLastOrderedCartInSession(request); // Xem xét có nên xóa ngay không

      if (lastOrder == null) {
         return "redirect:/shoppingCart";
      }
      model.addAttribute("lastOrderInfo", lastOrder);
      return "shoppingCartFinalize";
   }

   @RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
   public void productImage(HttpServletResponse response, @RequestParam("code") String code) throws IOException {
      if (code != null && !code.isEmpty()) {
         try {
            String imageUrl = productServiceBaseUrl + "/" + code + "/image";
            ResponseEntity<byte[]> imageResponse = restTemplate.getForEntity(imageUrl, byte[].class);
            if (imageResponse.getStatusCode() == HttpStatus.OK && imageResponse.getBody() != null) {
               String contentType = imageResponse.getHeaders().getFirst("Content-Type");
               response.setContentType(contentType != null ? contentType : "image/jpeg");
               response.getOutputStream().write(imageResponse.getBody());
            } else {
               response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
         } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Image not found from product-service for code: {}", code);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
         } catch (Exception e) {
            logger.error("Error fetching product image from service for code: {}", code, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         }
      } else {
         response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      }
   }
}