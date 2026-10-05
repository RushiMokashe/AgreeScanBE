package com.myagree.app.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/** A farmer's shopping cart. Adding a product that is already in the cart increases that line's quantity. */
@Entity
public class Cart {

    /** Orders of at least this many rupees ship free. */
    public static final int FREE_DELIVERY_THRESHOLD = 499;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private long farmerId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<CartItem> items = new ArrayList<>();

    protected Cart() {
    }

    public Cart(long farmerId) {
        this.farmerId = farmerId;
    }

    public void add(Product product, int quantity) {
        findItemFor(product).ifPresentOrElse(
                item -> item.increaseBy(quantity),
                () -> items.add(new CartItem(this, product, quantity)));
    }

    /** Removes a line; returns {@code false} when the cart has no line with that id. */
    public boolean remove(long itemId) {
        return items.removeIf(item -> Objects.equals(item.getId(), itemId));
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** The products in the cart that the depot has run out of since they were added. */
    public List<Product> outOfStockProducts() {
        return items.stream().map(CartItem::getProduct).filter(product -> !product.isInStock()).toList();
    }

    /** Total number of units across all lines. */
    public int itemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public long total() {
        return items.stream().mapToLong(CartItem::lineTotal).sum();
    }

    public boolean qualifiesForFreeDelivery() {
        return total() >= FREE_DELIVERY_THRESHOLD;
    }

    public Long getId() {
        return id;
    }

    public long getFarmerId() {
        return farmerId;
    }

    public List<CartItem> getItems() {
        return List.copyOf(items);
    }

    private Optional<CartItem> findItemFor(Product product) {
        return items.stream()
                .filter(item -> Objects.equals(item.getProduct().getId(), product.getId()))
                .findFirst();
    }
}
