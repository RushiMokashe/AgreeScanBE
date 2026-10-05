package com.myagree.app.store;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.farmer.FarmerService;
import com.myagree.app.store.dto.CartResponse;
import com.myagree.app.store.dto.OrderConfirmationResponse;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final FarmerService farmerService;
    private final Clock clock;

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
                       OrderRepository orderRepository, FarmerService farmerService, Clock clock) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.farmerService = farmerService;
        this.clock = clock;
    }

    /** The signed-in farmer's cart; empty when they have never added anything. */
    public CartResponse currentCart() {
        return findCart().map(StoreMapper::toResponse).orElseGet(StoreMapper::emptyCart);
    }

    /** Adds {@code quantity} units of a product, merging into the existing line for that product. */
    @Transactional
    public CartResponse addItem(long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> NotFoundException.of("Product", productId));
        Cart cart = findCart().orElseGet(() -> cartRepository.save(new Cart(currentFarmerId())));
        cart.add(product, quantity);
        cartRepository.flush(); // assigns ids to new lines before they are returned to the client
        return StoreMapper.toResponse(cart);
    }

    /** Removes one line from the cart. */
    @Transactional
    public CartResponse removeItem(long itemId) {
        Cart cart = findCart().orElseThrow(() -> NotFoundException.of("Cart item", itemId));
        if (!cart.remove(itemId)) {
            throw NotFoundException.of("Cart item", itemId);
        }
        return StoreMapper.toResponse(cart);
    }

    /**
     * Places a cash-on-delivery order for everything in the cart and empties it.
     *
     * @throws BadRequestException when the cart is empty
     */
    @Transactional
    public OrderConfirmationResponse checkout() {
        Cart cart = findCart().filter(candidate -> !candidate.isEmpty())
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        Order order = orderRepository.save(Order.placeFrom(cart, clock.instant()));
        cart.clear();
        return StoreMapper.toConfirmation(order);
    }

    private Optional<Cart> findCart() {
        return cartRepository.findByFarmerId(currentFarmerId());
    }

    private long currentFarmerId() {
        return farmerService.currentFarmer().id();
    }
}
