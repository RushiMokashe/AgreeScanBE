package com.myagree.app.store;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.myagree.app.common.i18n.IndianNumbers;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.common.i18n.Messages;
import com.myagree.app.store.dto.AdminProductResponse;
import com.myagree.app.store.dto.CartItemResponse;
import com.myagree.app.store.dto.CartResponse;
import com.myagree.app.store.dto.OrderConfirmationResponse;
import com.myagree.app.store.dto.OrderSummaryResponse;
import com.myagree.app.store.dto.OrderSummaryResponse.OrderLineResponse;
import com.myagree.app.store.dto.ProductResponse;
import com.myagree.app.store.dto.RxBundleResponse;
import com.myagree.app.store.dto.ShopOrderResponse;
import com.myagree.app.store.dto.ShopProductResponse;
import com.myagree.app.store.dto.ShopProfileResponse;
import com.myagree.app.store.dto.StoreCategoryResponse;
import com.myagree.app.store.dto.StoreHomeResponse;

/** Shows the store, carts and orders in the reader's language. */
@Component
class StoreMapper {

    private static final String ORDER_PLACED = "store.order.placed";
    private static final String PAYMENT_METHOD_PREFIX = "store.order.payment.";
    private static final String MORE_ITEMS = "store.cart.more";
    private static final String SUMMARY_SEPARATOR = " + ";
    private static final String NO_SUMMARY = "";
    /** A summary names this many products, then counts the rest ("A + B + 2 more"). */
    private static final int SUMMARY_NAMED_ITEMS = 2;

    private final Messages messages;
    private final StorePictures pictures;

    StoreMapper(Messages messages, StorePictures pictures) {
        this.messages = messages;
        this.pictures = pictures;
    }

    ProductResponse toResponse(Product product, Language language) {
        return new ProductResponse(
                product.getId(),
                product.getName().resolve(language),
                product.getShortName().resolve(language),
                product.getCategory(),
                resolve(product.getTag(), language),
                product.getTagTone(),
                resolve(product.getPackSize(), language),
                product.getDescription().resolve(language),
                product.getPrice(),
                product.getMrp(),
                pictures.imageUrl(product),
                product.getRating(),
                resolve(product.getImageBadge(), language),
                product.getImageBadgeTone(),
                product.getStockNote().resolve(language),
                product.getStockTone(),
                product.getFooterIcon(),
                product.getFooterText().resolve(language),
                product.getFooterTone(),
                product.isFlashDeal(),
                product.isInStock(),
                product.getBarcode(),
                product.getShop().getId(),
                product.getShop().getName());
    }

    StoreHomeResponse toHome(StoreDepot depot, @Nullable RxBundle rxBundle, long rxCount,
                                    List<StoreCategory> categories, List<Product> flashDeals, Language language) {
        return new StoreHomeResponse(
                depot.getName(),
                depot.isOpen(),
                depot.getDeliveryLabel().resolve(language),
                rxBundle != null ? toResponse(rxBundle, language) : null,
                Math.toIntExact(rxCount),
                categories.stream().map(category -> toResponse(category, language)).toList(),
                depot.getFlashDealEndsAt(),
                flashDeals.stream().map(product -> toResponse(product, language)).toList());
    }

    CartResponse toResponse(Cart cart, Language language) {
        List<CartItem> items = cart.getItems();
        return new CartResponse(
                items.stream().map(item -> toResponse(item, language)).toList(),
                cart.itemCount(),
                cart.total(),
                cart.qualifiesForFreeDelivery(),
                summarize(items.stream().map(item -> item.getProduct().getShortName()).toList(), language),
                cart.shop().map(Shop::getId).orElse(null),
                cart.shop().map(Shop::getName).orElse(null));
    }

    static CartResponse emptyCart() {
        return new CartResponse(List.of(), 0, 0, false, NO_SUMMARY, null, null);
    }

    /** "Order #12 placed • ₹730 • Cash on Delivery". */
    OrderConfirmationResponse toConfirmation(Order order, Language language) {
        return new OrderConfirmationResponse(
                order.getId(),
                order.getItemCount(),
                order.getTotal(),
                order.getStatus(),
                order.getPaymentMethod(),
                messages.get(ORDER_PLACED, language, String.valueOf(order.getId()), IndianNumbers.rupees(order.getTotal()),
                        messages.get(PAYMENT_METHOD_PREFIX + order.getPaymentMethod().name(), language)));
    }

    OrderSummaryResponse toSummary(Order order, Language language) {
        List<OrderLine> lines = order.getLines();
        return new OrderSummaryResponse(
                order.getId(),
                order.getPlacedAt(),
                order.getItemCount(),
                order.getTotal(),
                order.getStatus(),
                order.getPaymentMethod(),
                summarize(lines.stream().map(OrderLine::getShortName).toList(), language),
                toLines(order, language),
                order.getShopName());
    }

    /** An order as its shop sees it: with the customer to deliver to. */
    ShopOrderResponse toShopOrder(Order order, Language language) {
        List<OrderLine> lines = order.getLines();
        return new ShopOrderResponse(
                order.getId(),
                order.getPlacedAt(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getItemCount(),
                order.getTotal(),
                order.getStatus(),
                order.getPaymentMethod(),
                summarize(lines.stream().map(OrderLine::getShortName).toList(), language),
                toLines(order, language));
    }

    ShopProfileResponse toShopProfile(Shop shop) {
        return new ShopProfileResponse(shop.getId(), shop.getName(), shop.getPlace(), shop.getPhone(), shop.getUpiId(),
                pictures.upiQrUrl(shop), shop.acceptsScanAndPay());
    }

    /** A product with its texts in every language, as the shop portal edits it. */
    ShopProductResponse toShopProduct(Product product) {
        LocalizedText packSize = product.getPackSize();
        return new ShopProductResponse(
                product.getId(),
                LocalizedTextDto.from(product.getName()),
                product.getCategory(),
                packSize != null ? LocalizedTextDto.from(packSize) : null,
                LocalizedTextDto.from(product.getDescription()),
                product.getPrice(),
                product.getMrp(),
                product.isInStock(),
                product.getBarcode(),
                pictures.imageUrl(product));
    }

    AdminProductResponse toAdminResponse(Product product, Language language) {
        return new AdminProductResponse(
                product.getId(),
                product.getName().resolve(language),
                product.getCategory(),
                product.getPrice(),
                product.getMrp(),
                product.isFlashDeal(),
                product.isInStock(),
                pictures.imageUrl(product),
                product.getBarcode(),
                product.getShop().getName());
    }

    private static List<OrderLineResponse> toLines(Order order, Language language) {
        return order.getLines().stream()
                .map(line -> new OrderLineResponse(line.getName().resolve(language), line.getQuantity(),
                        line.getUnitPrice(), line.lineTotal()))
                .toList();
    }

    private RxBundleResponse toResponse(RxBundle bundle, Language language) {
        Product product = bundle.getProduct();
        return new RxBundleResponse(
                product.getId(),
                bundle.getScanId(),
                bundle.getLabel().resolve(language),
                bundle.getPrescribedAt(),
                product.getName().resolve(language),
                product.getDescription().resolve(language),
                product.getPrice(),
                Objects.requireNonNullElse(product.getMrp(), product.getPrice()),
                pictures.imageUrl(product),
                bundle.getGenuineLabel().resolve(language),
                bundle.getSubsidyLabel().resolve(language));
    }

    private static StoreCategoryResponse toResponse(StoreCategory category, Language language) {
        return new StoreCategoryResponse(
                category.getCategory(),
                category.getTitle().resolve(language),
                category.getLocalTitle().resolve(language),
                category.getDescription().resolve(language),
                category.getIcon(),
                category.getBadge().resolve(language));
    }

    private static CartItemResponse toResponse(CartItem item, Language language) {
        Product product = item.getProduct();
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName().resolve(language),
                product.getShortName().resolve(language),
                item.getQuantity(),
                item.unitPrice(),
                item.lineTotal());
    }

    /** "Mancozeb 500g + Doodh Dhara 5kg", or "A + B + 2 more" for more products; empty for none. */
    private String summarize(List<LocalizedText> shortNames, Language language) {
        String named = shortNames.stream()
                .limit(SUMMARY_NAMED_ITEMS)
                .map(name -> name.resolve(language))
                .collect(Collectors.joining(SUMMARY_SEPARATOR));
        int unnamed = shortNames.size() - SUMMARY_NAMED_ITEMS;
        return unnamed > 0 ? named + ' ' + messages.get(MORE_ITEMS, language, String.valueOf(unnamed)) : named;
    }

    private static @Nullable String resolve(@Nullable LocalizedText text, Language language) {
        return text != null ? text.resolve(language) : null;
    }
}
