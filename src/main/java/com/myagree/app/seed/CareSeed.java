package com.myagree.app.seed;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.care.AgroDealer;
import com.myagree.app.care.AgroDealerRepository;
import com.myagree.app.care.AgroHub;
import com.myagree.app.care.AgroHubRepository;
import com.myagree.app.care.Agronomist;
import com.myagree.app.care.AgronomistRepository;
import com.myagree.app.care.DealerCertification;
import com.myagree.app.care.DealerStock;
import com.myagree.app.care.OnlineOffer;

/** The Kem & Karmala hub, its stockists and the on-duty agronomist. */
@Component
public class CareSeed implements DemoSeed {

    /** The dealers stocking the Early Blight prescription. */
    public static final SeedKey<List<AgroDealer>> STOCKISTS = SeedKey.named("stockists");

    private final AgroHubRepository hubRepository;
    private final AgroDealerRepository dealerRepository;
    private final AgronomistRepository agronomistRepository;

    CareSeed(AgroHubRepository hubRepository, AgroDealerRepository dealerRepository,
             AgronomistRepository agronomistRepository) {
        this.hubRepository = hubRepository;
        this.dealerRepository = dealerRepository;
        this.agronomistRepository = agronomistRepository;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void seed(SeedContext context) {
        hubRepository.save(new AgroHub("Kem & Karmala Agro Hub", "Solapur Dist.", 5, "Mancozeb & Azoxystrobin",
                "/images/maps/kem-karmala-agro-hub.png",
                new OnlineOffer("Buy Online from AgriScan Mandi Depot", context.get(StoreSeed.CATALOG).rxBundle().getId())));
        agronomistRepository.save(new Agronomist("Dr. Suresh Patel", "Plant Pathologist • ICAR Certified",
                "/images/people/dr-suresh-patel.jpg", "1800999888", true,
                "Need assistance calibrating spray tank volume for high morning humidity? Connect directly with Dr. Patel."));
        context.put(STOCKISTS, dealerRepository.saveAll(List.of(
                new AgroDealer("Kisan Krishi Seva Kendra", DealerCertification.GOVT_CERTIFIED, 1.8,
                        "Karmala Road, Kem (Opp. APMC Mandi Gate)", 4.8, 140, "1800123456",
                        new DealerStock("Ready Stock Available", "₹280 / 500g",
                                "Indofil M-45 Mancozeb 75% WP • 500g & 1kg packs", "Batch No: IND-2024 • Expiry: Aug 2026")),
                new AgroDealer("Balaji Agro Chemicals & Seeds", DealerCertification.AUTHORIZED_RETAILER, 3.4,
                        "State Highway Junction, Karmala", 4.6, 88, "1800654321",
                        new DealerStock("In Stock • Organic Bio-fungicide", "Spares Avail.",
                                "Trichoderma viride & Knapsack Sprayer Spares", "Nozzles, Washers & Pressure Regulators")))));
    }
}
