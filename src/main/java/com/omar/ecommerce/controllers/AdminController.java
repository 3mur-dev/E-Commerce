package com.omar.ecommerce.controllers;

import com.omar.ecommerce.dtos.CategoryRequest;
import com.omar.ecommerce.dtos.OrderStats;
import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderStatus;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.services.CategoryService;
import com.omar.ecommerce.services.OrderService;
import com.omar.ecommerce.services.ProductService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;

    //ADMIN VIEW PAGE
    @GetMapping
    public String adminDashboard() {
        return "admin-dashboard";
    }

    /*
      Products methods
    */

    @GetMapping("/products")
    public String adminProducts(Model model, @RequestParam(required = false) String keyword) {

        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("products", productService.searchByName(keyword));
        } else {
            model.addAttribute("products", productService.findAll());
        }

        model.addAttribute("productRequest", new ProductRequest());
        model.addAttribute("keyword", keyword); // keep value in search bar
        model.addAttribute("categories", categoryService.findAll()); // <-- list of categories

        return "admin-products"; // Thymeleaf template
    }

    @GetMapping("/products/edit/{id}")
    public String editProduct(@PathVariable long id, Model model) {
        ProductResponse product = productService.getResponseById(id);

        ProductRequest productRequest = new ProductRequest();
        productRequest.setName(product.getName());
        productRequest.setPrice(product.getPrice());
        productRequest.setCategoryId(product.getCategoryId());
        productRequest.setStock(product.getStock());

        model.addAttribute("productRequest", productRequest);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("productId", id); // use this in form
        return "edit-product";
    }


    @PostMapping("/products/add")
    public String addProduct(@Valid @ModelAttribute ProductRequest request,
                             BindingResult result, RedirectAttributes ra,
                             @RequestParam(value = "image", required = false) MultipartFile image) {

        if (result.hasErrors()) {
            ra.addFlashAttribute("errors", result.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList());
            return "redirect:/admin/products";
        }

        // Validate image BEFORE service call
        if (image != null && !image.isEmpty()) {
            if (!isValidImage(image)) {
                ra.addFlashAttribute("error", "Invalid image: Only JPEG/PNG allowed, max 5MB");
                return "redirect:/admin/products";
            }
        }

        try {
            productService.create(request, image);  // Service handles null gracefully
            ra.addFlashAttribute("success", "Product added successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add product: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    private boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        long maxSize = 5 * 1024 * 1024L; // 5MB

        return contentType != null &&
                (contentType.equals("image/jpeg") || contentType.equals("image/png")) &&
                file.getSize() <= maxSize;
    }


    @PostMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable long id, RedirectAttributes ra) {
        try {
            productService.delete(id);
            ra.addFlashAttribute("success", "Product deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/products/update/{id}")
    public String updateProduct(@Valid @ModelAttribute("productRequest") ProductRequest productRequest,
                                BindingResult result,
                                @PathVariable long id,
                                Model model,
                                RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("productId", id);

            return "edit-product";
        }

        try {
            productService.update(id, productRequest);
            ra.addFlashAttribute("success", "Product updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/edit/" + id;
        }

        return "redirect:/admin/products";
    }
    /*
      Category methods
    */

    @GetMapping("/categories")
    public String categories(Model model) {

        model.addAttribute("categories", categoryService.findAll());

        return "admin-categories";
    }


    @PostMapping("/categories/add")
    public String addCategory(
            @Valid @ModelAttribute CategoryRequest request,
            BindingResult result,
            RedirectAttributes ra
    ) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Invalid category name");
            return "redirect:/admin/categories";
        }

        try {
            categoryService.add(request);
            ra.addFlashAttribute("success", "Category added");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/categories";
    }


    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            ra.addFlashAttribute("success", "Category deleted");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/categories";
    }
    @PostMapping("/increase-stock/{id}")
    public String increaseStock(@PathVariable Long id, @RequestParam int amount) {
        productService.increaseStock(id, amount);
        return "redirect:/admin/products";
    }

    @PostMapping("/decrease-stock/{id}")
    public String decreaseStock(@PathVariable Long id, @RequestParam int amount) {
        productService.decreaseStock(id, amount);
        return "redirect:/admin/products";
    }

    @PostMapping("/set-stock/{id}")
    public String setStock(@PathVariable Long id, @RequestParam int quantity) {
        productService.setStock(id, quantity);
        return "redirect:/admin/products";
    }

    /*
    Order Methods
    */
    @GetMapping("/orders")
    public String getAllOrders(@RequestParam(required = false) String status, Model model) {
        List<Order> orders;

        if (status != null && !status.isEmpty()) {
            orders = orderService.getOrdersByStatus(OrderStatus.valueOf(status));
        } else {
            orders = orderService.getAllOrders();
        }

        // Stats calculation
        long totalShipped = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.SHIPPED)
                .count();
        long totalPending = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .count();
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("orders", orders);
        model.addAttribute("totalShipped", totalShipped);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("status", status); // keep filter selected

        return "admin-orders";
    }

    @PostMapping("/orders/updateStatus")
    public String updateOrderStatus(@RequestParam Long orderId,
                                    @RequestParam OrderStatus status) {
        Order order = orderService.getOrderById(orderId);
        if (order != null) {
            order.setStatus(status);
            orderService.saveOrder(order);
        }
        return "redirect:/admin/orders";
    }
}