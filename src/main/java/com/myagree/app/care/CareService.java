package com.myagree.app.care;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.care.dto.AgronomistResponse;
import com.myagree.app.care.dto.NearbyStockResponse;
import com.myagree.app.care.dto.OnlineOfferResponse;
import com.myagree.app.common.NotFoundException;
import com.myagree.app.store.StoreService;
import com.myagree.app.store.dto.ProductResponse;

@Service
@Transactional(readOnly = true)
public class CareService {

    private final AgroHubRepository hubRepository;
    private final AgroDealerRepository dealerRepository;
    private final AgronomistRepository agronomistRepository;
    private final StoreService storeService;

    public CareService(AgroHubRepository hubRepository, AgroDealerRepository dealerRepository,
                       AgronomistRepository agronomistRepository, StoreService storeService) {
        this.hubRepository = hubRepository;
        this.dealerRepository = dealerRepository;
        this.agronomistRepository = agronomistRepository;
        this.storeService = storeService;
    }

    /** Dealers within the farmer's agro hub that stock the current prescription, nearest first. */
    public NearbyStockResponse nearbyStock() {
        AgroHub hub = localHub();
        List<AgroDealer> dealers = dealerRepository.findByDistanceKmLessThanEqualOrderByDistanceKmAsc(hub.getRadiusKm());
        return CareMapper.toResponse(hub, onlineOffer(hub), dealers);
    }

    /** How many local stores stock a prescription, as shown on a diagnosis ("2 local stores stock this Rx"). */
    public int countNearbyStockists() {
        return Math.toIntExact(dealerRepository.countByDistanceKmLessThanEqual(localHub().getRadiusKm()));
    }

    /** The agronomist farmers are connected to right now. */
    public AgronomistResponse agronomistOnDuty() {
        return agronomistRepository.findFirstByOrderByOnDutyDescIdAsc()
                .map(CareMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("No agronomist is available"));
    }

    private AgroHub localHub() {
        return hubRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No agro hub serves the farmer's area"));
    }

    /** The hub's online offer, priced from the store's current catalogue; {@code null} when the hub has none. */
    private @Nullable OnlineOfferResponse onlineOffer(AgroHub hub) {
        OnlineOffer offer = hub.getOnlineOffer();
        if (offer == null) {
            return null;
        }
        ProductResponse product = storeService.getProduct(offer.productId());
        return CareMapper.toResponse(offer, product);
    }
}
