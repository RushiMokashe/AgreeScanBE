package com.myagree.app.seed;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.common.Tone;
import com.myagree.app.store.Cart;
import com.myagree.app.store.CartRepository;
import com.myagree.app.store.Product;
import com.myagree.app.store.ProductCategory;
import com.myagree.app.store.ProductRepository;
import com.myagree.app.store.StoreCategory;
import com.myagree.app.store.StoreCategoryRepository;
import com.myagree.app.store.StoreDepot;
import com.myagree.app.store.StoreDepotRepository;

/** Depot, departments and products, plus the demo farmer's cart from the design (₹730). */
@Component
public class StoreSeed implements DemoSeed {

    /** The products other seeds refer to. */
    public record Catalog(Product mancozeb, Product mineralMix, Product rxBundle) {
    }

    public static final SeedKey<Catalog> CATALOG = SeedKey.named("store catalog");

    private static final Duration FLASH_DEAL_TIME_LEFT = Duration.ofHours(8).plusMinutes(42).plusSeconds(15);
    private static final String MANCOZEB_IMAGE = "/images/products/mancozeb-indofil-m45.jpg";

    private final ProductRepository productRepository;
    private final StoreCategoryRepository categoryRepository;
    private final StoreDepotRepository depotRepository;
    private final CartRepository cartRepository;
    private final Clock clock;

    StoreSeed(ProductRepository productRepository, StoreCategoryRepository categoryRepository,
              StoreDepotRepository depotRepository, CartRepository cartRepository, Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.depotRepository = depotRepository;
        this.cartRepository = cartRepository;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void seed(SeedContext context) {
        depotRepository.save(new StoreDepot(
                "Solapur Mandi Agro Depot", true, "24h Express", clock.instant().plus(FLASH_DEAL_TIME_LEFT)));
        categoryRepository.saveAll(departments());
        Catalog catalog = seedProducts();
        Cart cart = new Cart(context.get(FarmerSeed.FARMERS).rishikesh().getId());
        cart.add(catalog.mancozeb(), 1);
        cart.add(catalog.mineralMix(), 1);
        cartRepository.save(cart);
        context.put(CATALOG, catalog);
    }

    private static List<StoreCategory> departments() {
        return List.of(
                new StoreCategory(ProductCategory.CROP_MEDICINE, "Crop Medicines", "फसल सुरक्षा व कीटनाशक",
                        "Fungicides, Bio-sprays & Tonics", "vaccines", "140+ Items"),
                new StoreCategory(ProductCategory.SEEDS, "Hybrid Seeds", "प्रमाणित उन्नत बीज",
                        "Wheat HD-2967, Tomato, Cotton", "spa", "ICAR Certified"),
                new StoreCategory(ProductCategory.TOOLS, "Tools & Sprayers", "कृषि यंत्र व उपकरण",
                        "Battery pumps, Pruners, Drip", "agriculture", "Warranty"),
                new StoreCategory(ProductCategory.DAIRY, "Pashu Palan", "पशु आहार व देखभाल",
                        "Mineral mixtures, Calcium, Feed", "pets", "Vet Verified"));
    }

    /** Saves the flash deals first, in the design's order, then the prescription products. */
    private Catalog seedProducts() {
        Product mineralMix = mineralMix();
        Product mancozeb = mancozeb();
        Product rxBundle = blightRxBundle();
        productRepository.saveAll(List.of(tomatoSeeds(), batterySprayer(), mineralMix, hexaconazole(), mancozeb, rxBundle));
        return new Catalog(mancozeb, mineralMix, rxBundle);
    }

    private static Product tomatoSeeds() {
        return Product.builder("Syngenta Abhinav Tomato Seeds", "Syngenta Abhinav Seeds", ProductCategory.SEEDS)
                .tag("Blight Tolerant", Tone.SUCCESS)
                .packSize("10g Pkt")
                .description("F1 Hybrid • High firmness for long transit")
                .price(480, 550)
                .image("/images/products/syngenta-abhinav-tomato-seeds.jpg")
                .rating(4.8)
                .stockNote("In Stock", Tone.SUCCESS)
                .footer("schedule", "Delivers Tomorrow morning", Tone.NEUTRAL)
                .flashDeal()
                .build();
    }

    private static Product batterySprayer() {
        return Product.builder("Kisan Shakti 16L Battery Sprayer", "Kisan Shakti 16L Sprayer", ProductCategory.TOOLS)
                .tag("1-Yr Motor Guarantee", Tone.NEUTRAL)
                .description("12V/12Ah Dual Pump • Sprays 25-30 tanks")
                .price(2150, 2999)
                .image("/images/products/kisan-shakti-16l-sprayer.jpg")
                .imageBadge("28% OFF", Tone.ERROR)
                .stockNote("Only 4 Left", Tone.ERROR)
                .footer("done_all", "Cash on Delivery Available", Tone.SUCCESS)
                .flashDeal()
                .build();
    }

    private static Product mineralMix() {
        return Product.builder("Doodh Dhara Cattle Mineral Mix", "Doodh Dhara 5kg", ProductCategory.DAIRY)
                .tag("दूध उत्पादन वर्धक", Tone.WARNING)
                .packSize("5 kg Bucket")
                .description("Chelated Minerals + Vitamins AD3E & Calcium")
                .price(450, 520)
                .image("/images/products/doodh-dhara-mineral-mix.jpg")
                .imageBadge("Vet Seal", Tone.SUCCESS)
                .stockNote("+10% Milk Fat", Tone.SUCCESS)
                .footer("health_and_safety", "Dosage: 50g per day per cow", Tone.WARNING)
                .flashDeal()
                .build();
    }

    private static Product hexaconazole() {
        return Product.builder("Tata Contaf Plus (Hexaconazole 5% SC)", "Tata Contaf Plus 500ml", ProductCategory.CROP_MEDICINE)
                .tag("Systemic Fungicide", Tone.NEUTRAL)
                .packSize("500 ml")
                .description("Controls Sheath Blight, Tikka disease & Rust")
                .price(395, 460)
                .image("/images/products/tata-contaf-plus.jpg")
                .stockNote("Fast Dispatch", Tone.SUCCESS)
                .footer("verified_user", "QR Batch Genuine Verified", Tone.NEUTRAL)
                .flashDeal()
                .build();
    }

    private static Product mancozeb() {
        return Product.builder("Indofil M-45 Mancozeb 75% WP", "Mancozeb 500g", ProductCategory.CROP_MEDICINE)
                .tag("Contact Fungicide", Tone.NEUTRAL)
                .packSize("500 g")
                .description("Broad-spectrum contact protection against Early & Late Blight")
                .price(280, 320)
                .image(MANCOZEB_IMAGE)
                .stockNote("In Stock", Tone.SUCCESS)
                .footer("verified_user", "Recommended for Plot A Rx", Tone.NEUTRAL)
                .build();
    }

    private static Product blightRxBundle() {
        return Product.builder("Mancozeb 75% WP + Nozzle Kit", "Plot A Blight Rx Bundle", ProductCategory.CROP_MEDICINE)
                .tag("Doctor's Rx", Tone.SUCCESS)
                .packSize("500 g + Nozzle")
                .description("Indofil M-45 (500g) + Brass Mist Nozzle")
                .price(420, 530)
                .image(MANCOZEB_IMAGE)
                .imageBadge("Rx", Tone.SUCCESS)
                .stockNote("In Stock", Tone.SUCCESS)
                .footer("prescriptions", "Matches your Plot A diagnosis", Tone.SUCCESS)
                .build();
    }
}
