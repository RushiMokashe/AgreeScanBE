package com.myagree.app.store;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.store.dto.ProductResponse;
import com.myagree.app.store.dto.StoreHomeResponse;

@Service
@Transactional(readOnly = true)
public class StoreService {

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

    /** The store front: depot status, the latest prescription bundle, departments and flash deals. */
    public StoreHomeResponse home() {
        StoreDepot depot = depotRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No agro depot is configured"));
        return StoreMapper.toHome(
                depot,
                rxBundleRepository.findFirstByOrderByPrescribedAtDesc().orElse(null),
                Math.toIntExact(rxBundleRepository.count()),
                categoryRepository.findAllByOrderByIdAsc(),
                productRepository.findByFlashDealTrueOrderByIdAsc());
    }

    /**
     * Searches the whole catalogue.
     *
     * @param category only products of this department; {@code null} for all
     * @param query    case-insensitive text matched against name, short name, description and tag;
     *                 {@code null} or blank for no text filter
     */
    public List<ProductResponse> searchProducts(@Nullable ProductCategory category, @Nullable String query) {
        String pattern = query == null || query.isBlank() ? null : ProductRepository.containsPattern(query);
        return productRepository.search(category, pattern).stream()
                .map(StoreMapper::toResponse)
                .toList();
    }

    /** One catalogue product. */
    public ProductResponse getProduct(long productId) {
        return productRepository.findById(productId)
                .map(StoreMapper::toResponse)
                .orElseThrow(() -> NotFoundException.of("Product", productId));
    }
}
