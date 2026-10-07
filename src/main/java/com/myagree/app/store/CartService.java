package com.myagree.app.store;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.account.Account;
import com.myagree.app.account.AccountService;
import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.security.CurrentUser;
import com.myagree.app.common.security.CurrentUserProvider;
import com.myagree.app.store.dto.CartResponse;
import com.myagree.app.store.dto.OrderConfirmationResponse;

/** The signed-in farmer's cart and its checkout into an order. */
@Service
@Transactional(readOnly = true)
public class CartService {

    private static final String PRODUCT_NOT_FOUND = "store.product.not-found";
    private static final String OUT_OF_STOCK = "store.product.out-of-stock";
    private static final String ITEM_NOT_FOUND = "store.cart.item-not-found";
    private static final String UNAVAILABLE = "store.cart.unavailable";
    private static final String OTHER_SHOP = "store.cart.other-shop";
    private static final UserMessage EMPTY_CART = UserMessage.of("store.cart.empty");
    private static final String NAME_SEPARATOR = ", ";

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final CurrentUserProvider currentUserProvider;
    private final StoreMapper mapper;
    private final Clock clock;

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
                       OrderRepository orderRepository, AccountService accountService,
                       CurrentUserProvider currentUserProvider, StoreMapper mapper, Clock clock) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.accountService = accountService;
        this.currentUserProvider = currentUserProvider;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** The signed-in farmer's cart; empty when they have never added anything. */
    public CartResponse currentCart(Language language) {
        return findCart().map(cart -> mapper.toResponse(cart, language)).orElseGet(StoreMapper::emptyCart);
    }

    /**
     * Adds {@code quantity} units of a product, merging into the existing line for that product. A cart holds one
     * shop's products, since a farmer may pay the shop directly: a product of another shop replaces the cart's
     * contents only when {@code replaceCart} says the farmer agreed.
     *
     * @throws NotFoundException when there is no such product
     * @throws ConflictException when the shop has run out of it, or the cart holds another shop's products
     */
    @Transactional
    public CartResponse addItem(long productId, int quantity, boolean replaceCart, Language language) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(PRODUCT_NOT_FOUND, String.valueOf(productId))));
        if (!product.isInStock()) {
            throw new ConflictException(UserMessage.of(OUT_OF_STOCK, product.getShortName().resolve(language)));
        }
        Cart cart = findCart().orElseGet(() -> cartRepository.save(new Cart(currentFarmerId())));
        if (!cart.accepts(product)) {
            if (!replaceCart) {
                String cartShop = cart.shop().map(Shop::getName).orElseThrow();
                throw new ConflictException(UserMessage.of(OTHER_SHOP, cartShop, product.getShop().getName()));
            }
            cart.clear();
            cartRepository.flush(); // removes the old lines before a line for the same product could be added again
        }
        cart.add(product, quantity);
        cartRepository.flush(); // assigns ids to new lines before they are returned to the client
        return mapper.toResponse(cart, language);
    }

    /**
     * Removes one line from the cart.
     *
     * @throws NotFoundException when the cart has no such line
     */
    @Transactional
    public CartResponse removeItem(long itemId, Language language) {
        NotFoundException notFound = new NotFoundException(UserMessage.of(ITEM_NOT_FOUND, String.valueOf(itemId)));
        Cart cart = findCart().orElseThrow(() -> notFound);
        if (!cart.remove(itemId)) {
            throw notFound;
        }
        return mapper.toResponse(cart, language);
    }

    /**
     * Places an order for everything in the cart, in the signed-in account's name, and empties the cart. A
     * cash-on-delivery order is placed right away; an online order waits for its payment (POST /api/payments).
     *
     * @throws BadRequestException when the cart is empty
     * @throws ConflictException   when the depot has run out of a product in it
     */
    @Transactional
    public OrderConfirmationResponse checkout(PaymentMethod paymentMethod, Language language) {
        Cart cart = findCart().filter(candidate -> !candidate.isEmpty())
                .orElseThrow(() -> new BadRequestException(EMPTY_CART));
        List<Product> unavailable = cart.outOfStockProducts();
        if (!unavailable.isEmpty()) {
            throw new ConflictException(UserMessage.of(UNAVAILABLE, unavailable.stream()
                    .map(product -> product.getShortName().resolve(language))
                    .collect(Collectors.joining(NAME_SEPARATOR))));
        }
        CurrentUser user = currentUserProvider.get();
        Account customer = accountService.get(user.userId());
        Order order = orderRepository.save(
                Order.placeFrom(cart, customer.name(), customer.phone(), paymentMethod, clock.instant()));
        cart.clear();
        return mapper.toConfirmation(order, language);
    }

    private Optional<Cart> findCart() {
        return cartRepository.findByFarmerId(currentFarmerId());
    }

    private long currentFarmerId() {
        return currentUserProvider.get().requireFarmerId();
    }
}
