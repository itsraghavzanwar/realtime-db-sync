package com.orders.service;

import com.orders.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class OrderService {

    private final JdbcTemplate jdbc;

    @Autowired
    public OrderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }


    public List<Order> getAllOrders() {
        return jdbc.query(
            "SELECT id, customer_name, product_name, status, updated_at " +
            "FROM orders ORDER BY updated_at DESC",
            new OrderRowMapper()
        );
    }


    public void insertOrder(String customerName, String productName, String status) {
        jdbc.update(
            "INSERT INTO orders (customer_name, product_name, status) VALUES (?, ?, ?)",
            customerName, productName, status
        );
    }

    public void updateStatus(int id, String newStatus) {
        jdbc.update(
            "UPDATE orders SET status = ? WHERE id = ?",
            newStatus, id
        );
    }

    public void deleteOrder(int id) {
        jdbc.update("DELETE FROM orders WHERE id = ?", id);
    }

    private static class OrderRowMapper implements RowMapper<Order> {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Order(
                rs.getInt("id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getString("status"),
                rs.getTimestamp("updated_at").toLocalDateTime()
            );
        }
    }
}
