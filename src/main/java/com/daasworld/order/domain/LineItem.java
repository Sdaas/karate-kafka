package com.daasworld.order.domain;

public class LineItem {
    private int id;
    private int quantity;
    private int price;
    private int total=0;

    public LineItem() {
        // nothing to do here.
    }

    public LineItem(int id, int quantity, int price) {
        this.id = id;
        this.quantity = quantity;
        this.price = price;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) { this.id = id; }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getTotal() { return total;}
    public void setTotal(int total) { this.total = total; }

    @Override
    public String toString() {
        return "LineItem{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", price=" + price +
                ", total=" + total +
                '}';
    }
}
