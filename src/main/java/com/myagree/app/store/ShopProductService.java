package com.myagree.app.store;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.myagree.app.common.BadRequestException;
import com.myagree.app.common.ConflictException;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.common.i18n.LocalizedTextDto;
import com.myagree.app.common.i18n.UserMessage;
import com.myagree.app.common.media.PhotoStore;
import com.myagree.app.store.dto.ShopProductRequest;
import com.myagree.app.store.dto.ShopProductResponse;

/**
 * The shopkeeper portal's products: list, add, edit, mark in or out of stock, and photograph. Products are not deleted,
 * since past orders and carts refer to them; a product the shop no longer sells is marked out of stock.
 */
@Service
@Transactional(readOnly = true)
public class ShopProductService {

    private static final String PRODUCT_NOT_FOUND = "store.product.not-found";
    private static final String BARCODE_INVALID = "store.product.barcode-invalid";
    private static final String BARCODE_TAKEN = "store.shop.barcode-taken";
    private static final String SHOP_NOT_FOUND = "store.shop.not-found";
    private static final UserMessage MRP_BELOW_PRICE = UserMessage.of("store.product.mrp-below-price");

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final PhotoStore photoStore;
    private final StoreMapper mapper;

    ShopProductService(ProductRepository productRepository, ShopRepository shopRepository, PhotoStore photoStore,
                       StoreMapper mapper) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.photoStore = photoStore;
        this.mapper = mapper;
    }

    /** The shop's products, newest first. */
    public List<ShopProductResponse> products(long shopId) {
        return productRepository.findByShopIdOrderByIdDesc(shopId).stream().map(mapper::toShopProduct).toList();
    }

    /**
     * @throws NotFoundException when the shop has no such product
     */
    public ShopProductResponse product(long shopId, long productId) {
        return mapper.toShopProduct(find(shopId, productId));
    }

    /**
     * Lists a new product in the store.
     *
     * @throws BadRequestException when the MRP is below the price or the barcode is not a valid EAN-13 code
     * @throws ConflictException   when another product already has the barcode
     */
    @Transactional
    public ShopProductResponse create(long shopId, ShopProductRequest request) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(SHOP_NOT_FOUND, String.valueOf(shopId))));
        ShopListing listing = listing(request, null);
        return mapper.toShopProduct(productRepository.save(Product.listedBy(shop, listing)));
    }

    /**
     * Replaces a product's details with {@code request}.
     *
     * @throws NotFoundException   when the shop has no such product
     * @throws BadRequestException when the MRP is below the price or the barcode is not a valid EAN-13 code
     * @throws ConflictException   when another product already has the barcode
     */
    @Transactional
    public ShopProductResponse update(long shopId, long productId, ShopProductRequest request) {
        Product product = find(shopId, productId);
        product.updateListing(listing(request, product.getId()));
        return mapper.toShopProduct(product);
    }

    /** Marks a product in or out of stock; farmers cannot add an out-of-stock product to their cart. */
    @Transactional
    public ShopProductResponse changeStock(long shopId, long productId, boolean inStock) {
        Product product = find(shopId, productId);
        product.changeStock(inStock);
        return mapper.toShopProduct(product);
    }

    @Transactional
    public ShopProductResponse uploadPhoto(long shopId, long productId, MultipartFile image) {
        Product product = find(shopId, productId);
        product.replacePhoto(photoStore.store(StorePictures.PRODUCT_KIND, image));
        return mapper.toShopProduct(product);
    }

    /** What the shopkeeper entered, checked; {@code productId} is the product being edited, if any. */
    private ShopListing listing(ShopProductRequest request, @Nullable Long productId) {
        if (request.mrp() != null && request.mrp() < request.price()) {
            throw new BadRequestException(MRP_BELOW_PRICE);
        }
        String barcode = request.barcode();
        if (barcode != null) {
            if (!Ean13.isValid(barcode)) {
                throw new BadRequestException(UserMessage.of(BARCODE_INVALID, barcode));
            }
            productRepository.findByBarcode(barcode)
                    .filter(other -> !Objects.equals(other.getId(), productId))
                    .ifPresent(other -> {
                        throw new ConflictException(UserMessage.of(BARCODE_TAKEN, barcode));
                    });
        }
        LocalizedTextDto packSize = request.packSize();
        return new ShopListing(request.name().toLocalizedText(), request.category(),
                packSize != null ? packSize.toLocalizedText() : null, request.description().toLocalizedText(),
                request.price(), request.mrp(), request.inStock(), barcode);
    }

    private Product find(long shopId, long productId) {
        return productRepository.findByIdAndShopId(productId, shopId)
                .orElseThrow(() -> new NotFoundException(UserMessage.of(PRODUCT_NOT_FOUND, String.valueOf(productId))));
    }
}
