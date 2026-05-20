package com.orders.websocket;

import com.orders.model.Order;
import com.orders.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<Order> getAll() {
        return orderService.getAllOrders();
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Map<String, String> body) {
        String customerName = body.get("customerName");
        String productName  = body.get("productName");
        String status       = body.getOrDefault("status", "pending");
        orderService.insertOrder(customerName, productName, status);
        return ResponseEntity.ok("Order created – clients will be notified.");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        orderService.updateStatus(id, body.get("status"));
        return ResponseEntity.ok("Status updated – clients will be notified.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable int id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok("Order deleted – clients will be notified.");
    }
}
