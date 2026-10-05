package com.myagree.app.mandi;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myagree.app.common.NotFoundException;
import com.myagree.app.mandi.dto.BuyerInquiryResponse;
import com.myagree.app.mandi.dto.MandiOverviewResponse;

@Service
@Transactional(readOnly = true)
public class MandiService {

    private final MandiMarketRepository marketRepository;
    private final CommodityPriceRepository priceRepository;
    private final BuyerInquiryRepository inquiryRepository;

    public MandiService(MandiMarketRepository marketRepository, CommodityPriceRepository priceRepository,
                        BuyerInquiryRepository inquiryRepository) {
        this.marketRepository = marketRepository;
        this.priceRepository = priceRepository;
        this.inquiryRepository = inquiryRepository;
    }

    /** The farmer's home mandi: live status, price outlook, commodity rates and open buyer inquiries. */
    public MandiOverviewResponse overview() {
        MandiMarket market = marketRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("No mandi market is configured"));
        return MandiMapper.toOverview(
                market,
                priceRepository.findByMarketOrderByIdAsc(market),
                inquiryRepository.findByMarketOrderByIdAsc(market));
    }

    /** Registers the farmer's interest in a buyer inquiry. Responding twice is harmless. */
    @Transactional
    public BuyerInquiryResponse respondToInquiry(long inquiryId) {
        BuyerInquiry inquiry = inquiryRepository.findWithSpecsById(inquiryId)
                .orElseThrow(() -> NotFoundException.of("Buyer inquiry", inquiryId));
        inquiry.respond();
        return MandiMapper.toResponse(inquiry);
    }
}
