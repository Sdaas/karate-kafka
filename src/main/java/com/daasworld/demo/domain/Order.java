package com.daasworld.demo.domain;

public class Order {
    private static int nextOrderId = 0;

    private int id;
    private Customer customer;
    private LineItem lineItem;

    public Order(){

    }

    public Order(Customer c, LineItem lineItem){
        this.customer = c;
        this.lineItem = lineItem;
        this.id = nextOrderId;
        nextOrderId++;
    }

    public LineItem getLineItem() {
        return lineItem;
    }

    public void setLineItem(LineItem lineItem) {
        this.lineItem = lineItem;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
