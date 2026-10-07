package com.myagree.app.store;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.account.AccountService;
import com.myagree.app.common.ClockConfig;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.media.PhotoStore;
import com.myagree.app.store.dto.ShopDashboardResponse;
import com.myagree.app.store.dto.ShopOrderResponse;
import com.myagree.app.store.dto.ShopProfileRequest;
import com.myagree.app.store.dto.ShopProfileResponse;

/**
 * The shopkeeper portal's shop: its details and Scan & Pay set-up (UPI ID and QR), its home screen and its orders.
 * Every method is scoped to the signed-in shopkeeper's shop.
 */
@Service
@Transactional(readOnly = true)
public class ShopService {

    private static final String SHOP_NOT_FOUND = "store.shop.not-found";
    /** The orders the shop portal lists at most, newest first. */
    private static final int ORDERS_LIMIT = 100;
    private static final int RECENT_ORDERS = 5;

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final PhotoStore photoStore;
    private final StoreMapper mapper;
    private final Clock clock;

    ShopService(ShopRepository shopRepository, ProductRepository productRepository, OrderRepository orderRepository,
                AccountService accountService, PhotoStore photoStore, StoreMapper mapper, Clock clock) {
        this.shopRepository = shopRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.accountService = accountService;
        this.photoStore = photoStore;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Opens a shop for a shopkeeper account and links it to the account.
     *
     * @param phone the account's 10-digit mobile number, which farmers see
     */
    @Transactional
    public Shop open(long ownerUserId, String name, String place, String phone) {
        Shop shop = shopRepository.save(new Shop(ownerUserId, name, place, phone));
        accountService.linkShop(ownerUserId, shop.getId());
        return shop;
    }

    public ShopProfileResponse profile(long shopId) {
        return mapper.toShopProfile(find(shopId));
    }

    /** Changes the shop's name, place and UPI ID; a blank UPI ID removes it. */
    @Transactional
    public ShopProfileResponse updateProfile(long shopId, ShopProfileRequest request) {
        Shop shop = find(shopId);
        shop.updateProfile(request.name().strip(), request.place().strip(), blankToNull(request.upiId()));
        return mapper.toShopProfile(shop);
    }

    /** Shows {@code image} (a photo or screenshot of the shop's UPI QR) to farmers who pay by Scan & Pay. */
    @Transactional
    public ShopProfileResponse uploadUpiQr(long shopId, MultipartFile image) {
        Shop shop = find(shopId);
        shop.replaceUpiQr(photoStore.store(StorePictures.SHOP_QR_KIND, image));
        return mapper.toShopProfile(shop);
    }

    @Transactional
    public ShopProfileResponse removeUpiQr(long shopId) {
        Shop shop = find(shopId);
        shop.removeUpiQr();
        return mapper.toShopProfile(shop);
    }

    /** Today's orders and sales (India time), payments to confirm, the catalogue's size, and the latest orders. */
    public ShopDashboardResponse dashboard(long shopId, Language language) {
        Shop shop = find(shopId);
        Instant today = LocalDate.ofInstant(clock.instant(), ClockConfig.FARM_ZONE).atStartOfDay(ClockConfig.FARM_ZONE)
                .toInstant();
        return new ShopDashboardResponse(
                shop.getName(),
                Math.toIntExact(orderRepository.countByShopIdAndPlacedAtGreaterThanEqual(shopId, today)),
                orderRepository.sumTotalForShopSince(shopId, today, OrderStatus.CANCELLED),
                Math.toIntExact(orderRepository.countByShopIdAndStatus(shopId, OrderStatus.VERIFYING_PAYMENT)),
                Math.toIntExact(productRepository.countByShopId(shopId)),
                Math.toIntExact(productRepository.countByShopIdAndInStockFalse(shopId)),
                shop.acceptsScanAndPay(),
                orders(shopId, null, RECENT_ORDERS, language));
    }

    /** The shop's orders, newest first, optionally only those in {@code status}. */
    public List<ShopOrderResponse> orders(long shopId, @Nullable OrderStatus status, Language language) {
        return orders(shopId, status, ORDERS_LIMIT, language);
    }

    private List<ShopOrderResponse> orders(long shopId, @Nullable OrderStatus status, int limit, Language language) {
        return orderRepository.findForShop(shopId, status, PageRequest.of(0, limit)).stream()
                .map(order -> mapper.toShopOrder(order, language))
                .toList();
    }

    private Shop find(long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(SHOP_NOT_FOUND, String.valueOf(shopId))));
    }

    private static @Nullable String blankToNull(@Nullable String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
