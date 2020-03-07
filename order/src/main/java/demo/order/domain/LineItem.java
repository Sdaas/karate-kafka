package demo.order.domain;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineItem lineItem = (LineItem) o;
        return id == lineItem.id &&
                quantity == lineItem.quantity &&
                price == lineItem.price &&
                total == lineItem.total;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, quantity, price, total);
    }
}
