package com.myagree.app.common.spi;

import java.util.Optional;

import com.myagree.app.common.i18n.Language;

/**
 * Tells the voice assistant what one slice knows about a farmer's situation, so answers can be specific ("Plot A has
 * Early Blight; the next spray is due in 3 days").
 *
 * <p><b>Implemented by</b> the farm slice (plots, recent scans, treatment plans), the market slice (prices at the
 * farmer's preferred mandi) and the rental slice (the farmer's open bookings). <b>Consumed by</b> the assistant slice,
 * which injects {@code List<FarmContextContributor>} and adds every section returned to the context of each question.
 *
 * <p><b>Contract:</b> read-only and quick, because it runs for every question. It returns empty when the slice has
 * nothing worth saying about this farmer, and never throws for missing data. It shares only the farmer's own records
 * and public market data.
 */
public interface FarmContextContributor {

    /**
     * @param farmerId the farmer asking
     * @param language the language of the question and of the answer
     */
    Optional<FarmContextSection> contribute(long farmerId, Language language);
}
