package com.myagree.app.store;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.store.dto.AdminProductResponse;
import com.myagree.app.store.dto.AdminProductUpdateRequest;
import com.myagree.app.store.dto.ProductResponse;
import com.myagree.app.store.dto.StoreHomeResponse;

/** The agro store's catalogue: the store front, search, barcode lookup and the admin's prices. */
@Service
@Transactional(readOnly = true)
public class StoreService {

    private static final UserMessage NO_DEPOT = UserMessage.of("store.depot.none");
    private static final String PRODUCT_NOT_FOUND = "store.product.not-found";
    private static final String BARCODE_INVALID = "store.product.barcode-invalid";
    private static final String BARCODE_UNKNOWN = "store.product.barcode-unknown";
    private static final UserMessage MRP_BELOW_PRICE = UserMessage.of("store.product.mrp-below-price");

    private final StoreDepotRepository depotRepository;
    private final StoreCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final RxBundleRepository rxBundleRepository;

    public StoreService(StoreDepotRepository depotRepository, StoreCategoryRepository categoryRepository,
                        ProductRepository productRepository, RxBundleRepository rxBundleRepository) {
        this.depotRepository = depotRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.rxBundleRepository = rxBundleRepository;
    }

    /** The store front: depot status, the farmer's latest prescription bundle, departments and flash deals. */
    public StoreHomeResponse home(long farmerId, Language language) {
        StoreDepot depot = depotRepository.findFirstByOrderByIdAsc().orElseThrow(() -> new NotFoundException(NO_DEPOT));
        return StoreMapper.toHome(
                depot,
                rxBundleRepository.findFirstByFarmerIdOrderByPrescribedAtDesc(farmerId).orElse(null),
                rxBundleRepository.countByFarmerId(farmerId),
                categoryRepository.findAllByOrderByIdAsc(),
                productRepository.findByFlashDealTrueOrderByIdAsc(),
                language);
    }

    /**
     * Searches the whole catalogue.
     *
     * @param category only products of this department; {@code null} for all
     * @param query    case-insensitive text matched against name, short name, description and tag in every language;
     *                 {@code null} or blank for no text filter
     */
    public List<ProductResponse> searchProducts(@Nullable ProductCategory category, @Nullable String query,
                                                Language language) {
        String pattern = query == null || query.isBlank() ? null : ProductRepository.containsPattern(query);
        return productRepository.search(category, pattern).stream()
                .map(product -> StoreMapper.toResponse(product, language))
                .toList();
    }

    /**
     * One catalogue product.
     *
     * @throws NotFoundException when there is no such product
     */
    public ProductResponse getProduct(long productId, Language language) {
        return StoreMapper.toResponse(find(productId), language);
    }

    /**
     * The product whose pack carries {@code code}.
     *
     * @throws BadRequestException when the code is not an EAN-13 barcode
     * @throws NotFoundException   when no product carries it
     */
    public ProductResponse productByBarcode(String code, Language language) {
        String barcode = code.strip();
        if (!Ean13.isValid(barcode)) {
            throw new BadRequestException(UserMessage.of(BARCODE_INVALID, barcode));
        }
        return productRepository.findByBarcode(barcode)
                .map(product -> StoreMapper.toResponse(product, language))
                .orElseThrow(() -> new NotFoundException(UserMessage.of(BARCODE_UNKNOWN, barcode)));
    }

    /** Every product with its price and availability, for the admin portal. */
    public List<AdminProductResponse> adminProducts(Language language) {
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(product -> StoreMapper.toAdminResponse(product, language))
                .toList();
    }

    /**
     * Changes a product's price, MRP, flash deal or stock; fields left out stay as they are.
     *
     * @throws NotFoundException   when there is no such product
     * @throws BadRequestException when the MRP would be lower than the price
     */
    @Transactional
    public AdminProductResponse adminUpdate(long productId, AdminProductUpdateRequest update, Language language) {
        Product product = find(productId);
        if (update.price() != null || update.mrpGiven()) {
            int price = update.price() != null ? update.price() : product.getPrice();
            Integer mrp = update.mrpGiven() ? update.mrp() : product.getMrp();
            if (mrp != null && mrp < price) {
                throw new BadRequestException(MRP_BELOW_PRICE);
            }
            product.reprice(price, mrp);
        }
        if (update.flashDeal() != null) {
            product.changeFlashDeal(update.flashDeal());
        }
        if (update.inStock() != null) {
            product.changeStock(update.inStock());
        }
        return StoreMapper.toAdminResponse(product, language);
    }

    private Product find(long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(PRODUCT_NOT_FOUND, String.valueOf(productId))));
    }
}
