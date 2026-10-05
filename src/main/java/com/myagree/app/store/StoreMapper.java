package com.myagree.app.store;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.myagree.app.store.dto.CartItemResponse;
import com.myagree.app.store.dto.CartResponse;
import com.myagree.app.store.dto.OrderConfirmationResponse;
import com.myagree.app.store.dto.ProductResponse;
import com.myagree.app.store.dto.RxBundleResponse;
import com.myagree.app.store.dto.StoreCategoryResponse;
import com.myagree.app.store.dto.StoreHomeResponse;

final class StoreMapper {

    private static final String ORDER_PLACED_MESSAGE = "Order #%d placed • %s • Cash on Delivery";
    private static final String RUPEE_SIGN = "₹";
    /** Digits that close the rupee amount; everything before them is grouped in pairs (lakh, crore). */
    private static final int LAST_GROUP_DIGITS = 3;
    /** Matches the positions inside the leading digits where a lakh/crore separator goes. */
    private static final Pattern PAIR_BOUNDARY = Pattern.compile("\\B(?=(\\d{2})+$)");
    private static final String SUMMARY_SEPARATOR = " + ";
    /** The cart bar names this many products, then counts the rest ("A + B + 2 more"). */
    private static final int SUMMARY_NAMED_ITEMS = 2;

    private StoreMapper() {
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getShortName(),
                product.getCategory(),
                product.getTag(),
                product.getTagTone(),
                product.getPackSize(),
                product.getDescription(),
                product.getPrice(),
                product.getMrp(),
                product.getImageUrl(),
                product.getRating(),
                product.getImageBadge(),
                product.getImageBadgeTone(),
                product.getStockNote(),
                product.getStockTone(),
                product.getFooterIcon(),
                product.getFooterText(),
                product.getFooterTone(),
                product.isFlashDeal());
    }

    static StoreHomeResponse toHome(StoreDepot depot, @Nullable RxBundle rxBundle, int rxCount,
                                    List<StoreCategory> categories, List<Product> flashDeals) {
        return new StoreHomeResponse(
                depot.getName(),
                depot.isOpen(),
                depot.getDeliveryLabel(),
                rxBundle != null ? toResponse(rxBundle) : null,
                rxCount,
                categories.stream().map(StoreMapper::toResponse).toList(),
                depot.getFlashDealEndsAt(),
                flashDeals.stream().map(StoreMapper::toResponse).toList());
    }

    static CartResponse toResponse(Cart cart) {
        List<CartItem> items = cart.getItems();
        return new CartResponse(
                items.stream().map(StoreMapper::toResponse).toList(),
                cart.itemCount(),
                cart.total(),
                cart.qualifiesForFreeDelivery(),
                summarize(items));
    }

    static CartResponse emptyCart() {
        return new CartResponse(List.of(), 0, 0, false, "");
    }

    static OrderConfirmationResponse toConfirmation(Order order) {
        return new OrderConfirmationResponse(
                order.getId(),
                order.getItemCount(),
                order.getTotal(),
                order.getStatus(),
                ORDER_PLACED_MESSAGE.formatted(order.getId(), rupees(order.getTotal())));
    }

    private static RxBundleResponse toResponse(RxBundle bundle) {
        Product product = bundle.getProduct();
        return new RxBundleResponse(
                product.getId(),
                bundle.getScanId(),
                bundle.getLabel(),
                bundle.getPrescribedAt(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                Objects.requireNonNullElse(product.getMrp(), product.getPrice()),
                product.getImageUrl(),
                bundle.getGenuineLabel(),
                bundle.getSubsidyLabel());
    }

    private static StoreCategoryResponse toResponse(StoreCategory category) {
        return new StoreCategoryResponse(
                category.getCategory(),
                category.getTitle(),
                category.getLocalTitle(),
                category.getDescription(),
                category.getIcon(),
                category.getBadge());
    }

    private static CartItemResponse toResponse(CartItem item) {
        Product product = item.getProduct();
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getShortName(),
                item.getQuantity(),
                item.unitPrice(),
                item.lineTotal());
    }

    /** "Mancozeb 500g + Doodh Dhara 5kg", or "A + B + 2 more" for longer carts; empty for an empty cart. */
    private static String summarize(List<CartItem> items) {
        String named = items.stream()
                .limit(SUMMARY_NAMED_ITEMS)
                .map(item -> item.getProduct().getShortName())
                .collect(Collectors.joining(SUMMARY_SEPARATOR));
        int unnamed = items.size() - SUMMARY_NAMED_ITEMS;
        return unnamed > 0 ? named + SUMMARY_SEPARATOR + unnamed + " more" : named;
    }

    /**
     * Rupees with Indian digit grouping, e.g. "₹1,08,230". The JDK's number formats support a single grouping
     * size only, so the en-IN pattern (#,##,##0) has to be applied by hand.
     */
    static String rupees(long amount) {
        String digits = Long.toString(amount);
        if (digits.length() <= LAST_GROUP_DIGITS) {
            return RUPEE_SIGN + digits;
        }
        int split = digits.length() - LAST_GROUP_DIGITS;
        String leading = PAIR_BOUNDARY.matcher(digits.substring(0, split)).replaceAll(",");
        return RUPEE_SIGN + leading + "," + digits.substring(split);
    }
}
