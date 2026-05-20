package com.orders.model;

public class OrderEvent {

    private String operation;
    private Order data;

    public OrderEvent() {
    }

    public OrderEvent(String operation, Order data) {
        this.operation = operation;
        this.data = data;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String v) {
        this.operation = v;
    }

    public Order getData() {
        return data;
    }

    public void setData(Order v) {
        this.data = v;
    }
}
