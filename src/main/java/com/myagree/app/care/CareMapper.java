package com.myagree.app.care;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.myagree.app.care.dto.AgroDealerResponse;
import com.myagree.app.care.dto.AgronomistResponse;
import com.myagree.app.care.dto.DealerStockResponse;
import com.myagree.app.care.dto.NearbyStockResponse;
import com.myagree.app.care.dto.OnlineOfferResponse;
import com.myagree.app.store.dto.ProductResponse;

final class CareMapper {

    private CareMapper() {
    }

    static NearbyStockResponse toResponse(AgroHub hub, @Nullable OnlineOfferResponse onlineOffer, List<AgroDealer> dealers) {
        return new NearbyStockResponse(
                hub.getName(),
                hub.getDistrict(),
                hub.getRadiusKm(),
                hub.getProductsLabel(),
                hub.getMapImageUrl(),
                onlineOffer,
                dealers.stream().map(CareMapper::toResponse).toList());
    }

    static OnlineOfferResponse toResponse(OnlineOffer offer, ProductResponse product) {
        return new OnlineOfferResponse(offer.label(), product.price(), product.id());
    }

    static AgronomistResponse toResponse(Agronomist agronomist) {
        return new AgronomistResponse(
                agronomist.getId(),
                agronomist.getName(),
                agronomist.getTitle(),
                agronomist.getPhotoUrl(),
                agronomist.getPhone(),
                agronomist.isOnDuty(),
                agronomist.getPitch());
    }

    private static AgroDealerResponse toResponse(AgroDealer dealer) {
        DealerStock stock = dealer.getStock();
        return new AgroDealerResponse(
                dealer.getId(),
                dealer.getName(),
                dealer.getCertification(),
                dealer.getDistanceKm(),
                dealer.getAddress(),
                dealer.getRating(),
                dealer.getReviewCount(),
                dealer.getPhone(),
                new DealerStockResponse(stock.headline(), stock.priceLabel(), stock.detail(), stock.note()));
    }
}
