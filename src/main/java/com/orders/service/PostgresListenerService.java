package com.orders.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orders.model.Order;
import com.orders.model.OrderEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PostgresListenerService {

    private static final Logger log = LoggerFactory.getLogger(PostgresListenerService.class);

    private static final String CHANNEL = "orders_channel";
    private static final String WS_TOPIC = "/topic/orders";
    private static final long POLL_MS = 500;
    private static final long RECONNECT_SECS = 5;

    private final DataSource dataSource;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper mapper;

    private Connection listenConnection;
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    public PostgresListenerService(DataSource dataSource,
            SimpMessagingTemplate messagingTemplate) {
        this.dataSource = dataSource;
        this.messagingTemplate = messagingTemplate;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pg-listener");
            t.setDaemon(true);
            return t;
        });
        connectAndListen();
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        scheduler.shutdownNow();
        closeConnection();
        log.info("PostgreSQL listener stopped.");
    }

    private void connectAndListen() {
        try {
            listenConnection = dataSource.getConnection();
            listenConnection.setAutoCommit(true);

            try (Statement stmt = listenConnection.createStatement()) {
                stmt.execute("LISTEN " + CHANNEL);
            }

            running.set(true);
            log.info("Listening on PostgreSQL channel '{}'", CHANNEL);

            scheduler.scheduleWithFixedDelay(
                    this::pollNotifications,
                    0, POLL_MS, TimeUnit.MILLISECONDS
            );

        } catch (Exception e) {
            log.error("Failed to connect to PostgreSQL for LISTEN: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        log.info("Scheduling reconnect in {} seconds...", RECONNECT_SECS);
        scheduler.schedule(this::connectAndListen, RECONNECT_SECS, TimeUnit.SECONDS);
    }

    private void closeConnection() {
        try {
            if (listenConnection != null && !listenConnection.isClosed()) {
                listenConnection.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void pollNotifications() {
        if (!running.get()) {
            return;
        }

        try {
            PGConnection pgConn = listenConnection.unwrap(PGConnection.class);

            try (Statement keepalive = listenConnection.createStatement()) {
                keepalive.execute("SELECT 1");
            }

            PGNotification[] notifications = pgConn.getNotifications();

            if (notifications != null) {
                for (PGNotification notification : notifications) {
                    log.debug("Received notification on channel '{}': {}",
                            notification.getName(),
                            notification.getParameter());
                    handleNotification(notification.getParameter());
                }
            }

        } catch (Exception e) {
            log.error("Lost PostgreSQL LISTEN connection: {}", e.getMessage());
            running.set(false);
            closeConnection();
            scheduleReconnect();
        }
    }

    private void handleNotification(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            String operation = root.get("operation").asText();
            JsonNode dataNode = root.get("data");

            Order order = parseOrder(dataNode);
            OrderEvent event = new OrderEvent(operation, order);

            messagingTemplate.convertAndSend(WS_TOPIC, event);
            log.info("Pushed {} event for order id={} to WebSocket clients",
                    operation, order.getId());

        } catch (Exception e) {
            log.error("Failed to parse notification payload: {}", e.getMessage());
        }
    }

    private Order parseOrder(JsonNode node) {
        Order order = new Order();
        order.setId(node.get("id").asInt());
        order.setCustomerName(node.get("customer_name").asText());
        order.setProductName(node.get("product_name").asText());
        order.setStatus(node.get("status").asText());

        String ts = node.get("updated_at").asText();
        order.setUpdatedAt(LocalDateTime.parse(ts));

        return order;
    }
}
