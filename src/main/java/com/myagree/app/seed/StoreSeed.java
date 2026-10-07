package com.myagree.app.seed;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.account.Account;
import com.myagree.app.common.Tone;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.store.Cart;
import com.myagree.app.store.CartRepository;
import com.myagree.app.store.Product;
import com.myagree.app.store.ProductCategory;
import com.myagree.app.store.ProductRepository;
import com.myagree.app.store.Shop;
import com.myagree.app.store.ShopService;
import com.myagree.app.store.dto.ShopProfileRequest;
import com.myagree.app.store.StoreCategory;
import com.myagree.app.store.StoreCategoryRepository;
import com.myagree.app.store.StoreDepot;
import com.myagree.app.store.StoreDepotRepository;

/**
 * The agro store: the depot, its four departments, sixteen products (the six of the Stitch design first, exactly as it
 * shows them in English) with their EAN-13 barcodes, and the demo farmer's cart from the design (₹730). Every text is
 * in English, Marathi and Hindi; brand names stay as written.
 */
@Component
public class StoreSeed implements DemoSeed {

    /** The products other seeds refer to. */
    public record Catalog(Product mancozeb, Product mineralMix, Product rxBundle) {
    }

    public static final SeedKey<Catalog> CATALOG = SeedKey.named("store catalog");

    /** A UPI handle no UPI app knows: demo shops can show a real-looking QR that can never take real money. */
    private static final String DEMO_UPI_HANDLE = "@agriscandemo";
    private static final Duration FLASH_DEAL_TIME_LEFT = Duration.ofHours(8).plusMinutes(42).plusSeconds(15);
    private static final String MANCOZEB_IMAGE = "/images/products/mancozeb-indofil-m45.jpg";
    private static final LocalizedText IN_STOCK = text("In Stock", "स्टॉकमध्ये", "स्टॉक में");
    private static final LocalizedText CASH_ON_DELIVERY = text("Cash on Delivery Available",
            "डिलिव्हरीवेळी रोख पैसे देण्याची सोय", "डिलीवरी पर नकद भुगतान उपलब्ध");

    private final ProductRepository productRepository;
    private final StoreCategoryRepository categoryRepository;
    private final StoreDepotRepository depotRepository;
    private final CartRepository cartRepository;
    private final ShopService shopService;
    private final Clock clock;

    StoreSeed(ProductRepository productRepository, StoreCategoryRepository categoryRepository,
              StoreDepotRepository depotRepository, CartRepository cartRepository, ShopService shopService, Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.depotRepository = depotRepository;
        this.cartRepository = cartRepository;
        this.shopService = shopService;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void seed(SeedContext context) {
        depotRepository.save(new StoreDepot("Solapur Mandi Agro Depot", true,
                text("24h Express", "24 तासांत डिलिव्हरी", "24 घंटे में डिलीवरी"),
                clock.instant().plus(FLASH_DEAL_TIME_LEFT)));
        categoryRepository.saveAll(departments());
        AccountSeed.DemoAccounts accounts = context.get(AccountSeed.ACCOUNTS);
        Shop depot = shop(accounts.sanjayKulkarni(), "Solapur Mandi Agro Depot", "Solapur", "solapur.agro");
        Shop karmala = shop(accounts.maheshJadhav(), "Karmala Krishi Seva Kendra", "Karmala, Solapur", "karmala.krishi");
        Catalog catalog = seedProducts(depot, karmala);
        Cart cart = new Cart(context.get(FarmerSeed.FARMERS).rishikesh().getId());
        cart.add(catalog.mancozeb(), 1);
        cart.add(catalog.mineralMix(), 1);
        cartRepository.save(cart);
        context.put(CATALOG, catalog);
    }

    private static List<StoreCategory> departments() {
        return List.of(
                new StoreCategory(ProductCategory.CROP_MEDICINE,
                        text("Crop Medicines", "पीक औषधे", "फसल दवाइयाँ"),
                        text("फसल सुरक्षा व कीटनाशक", "पीक संरक्षण व कीटकनाशके", "फसल सुरक्षा व कीटनाशक"),
                        text("Fungicides, Bio-sprays & Tonics", "बुरशीनाशके, जैविक फवारे व टॉनिक", "फफूंदनाशी, जैविक स्प्रे व टॉनिक"),
                        "vaccines", text("140+ Items", "140+ उत्पादने", "140+ उत्पाद")),
                new StoreCategory(ProductCategory.SEEDS,
                        text("Hybrid Seeds", "संकरित बियाणे", "संकर बीज"),
                        text("प्रमाणित उन्नत बीज", "प्रमाणित सुधारित बियाणे", "प्रमाणित उन्नत बीज"),
                        text("Wheat HD-2967, Tomato, Cotton", "गहू HD-2967, टोमॅटो, कापूस", "गेहूँ HD-2967, टमाटर, कपास"),
                        "spa", text("ICAR Certified", "ICAR प्रमाणित", "ICAR प्रमाणित")),
                new StoreCategory(ProductCategory.TOOLS,
                        text("Tools & Sprayers", "अवजारे व फवारणी पंप", "औज़ार व स्प्रेयर"),
                        text("कृषि यंत्र व उपकरण", "कृषी यंत्रे व उपकरणे", "कृषि यंत्र व उपकरण"),
                        text("Battery pumps, Pruners, Drip", "बॅटरी पंप, छाटणी कात्री, ठिबक", "बैटरी पंप, कटाई कैंची, ड्रिप"),
                        "agriculture", text("Warranty", "वॉरंटी", "वारंटी")),
                new StoreCategory(ProductCategory.DAIRY,
                        text("Pashu Palan", "पशुपालन", "पशु पालन"),
                        text("पशु आहार व देखभाल", "पशुखाद्य व निगा", "पशु आहार व देखभाल"),
                        text("Mineral mixtures, Calcium, Feed", "खनिज मिश्रण, कॅल्शियम, पशुखाद्य", "खनिज मिश्रण, कैल्शियम, चारा"),
                        "pets", text("Vet Verified", "पशुवैद्यांनी तपासलेले", "पशु चिकित्सक द्वारा जाँचा")));
    }

    /**
     * A demo shop with Scan & Pay set up. Its UPI ID uses a handle no UPI app knows, so its QR can never send real
     * money to anyone; testers pay by entering any 12-digit UPI reference.
     */
    private Shop shop(Account shopkeeper, String name, String place, String upiName) {
        Shop shop = shopService.open(shopkeeper.id(), name, place, shopkeeper.phone());
        shopService.updateProfile(shop.getId(), new ShopProfileRequest(name, place, upiName + DEMO_UPI_HANDLE));
        return shop;
    }

    /**
     * Saves the flash deals first, in the design's order, then the prescription products, then the rest. The depot
     * sells crop medicines, seeds and cattle care; the Karmala shop sells tools and sprayers.
     */
    private Catalog seedProducts(Shop depot, Shop karmala) {
        Product mineralMix = mineralMix(depot);
        Product mancozeb = mancozeb(depot);
        Product rxBundle = blightRxBundle(depot);
        productRepository.saveAll(List.of(tomatoSeeds(depot), batterySprayer(karmala), mineralMix, hexaconazole(depot),
                mancozeb, rxBundle, imidacloprid(depot), neemOil(depot), btCottonSeeds(depot), wheatSeeds(depot),
                onionSeeds(depot), handSprayer(karmala), dripKit(karmala), secateurs(karmala), calciumSupplement(depot),
                cattleFeed(depot)));
        return new Catalog(mancozeb, mineralMix, rxBundle);
    }

    private static Product tomatoSeeds(Shop shop) {
        return Product.builder(shop,
                        text("Syngenta Abhinav Tomato Seeds", "सिंजेंटा अभिनव टोमॅटो बियाणे", "सिंजेंटा अभिनव टमाटर बीज"),
                        text("Syngenta Abhinav Seeds", "सिंजेंटा अभिनव बियाणे", "सिंजेंटा अभिनव बीज"), ProductCategory.SEEDS)
                .tag(text("Blight Tolerant", "करपा सहनशील", "झुलसा सहनशील"), Tone.SUCCESS)
                .packSize(text("10g Pkt", "10 ग्रॅम पाकीट", "10 ग्राम पैकेट"))
                .description(text("F1 Hybrid • High firmness for long transit", "F1 संकरित • लांबच्या वाहतुकीसाठी घट्ट फळे",
                        "F1 संकर • लंबी ढुलाई के लिए सख़्त फल"))
                .price(480, 550)
                .image("/images/products/syngenta-abhinav-tomato-seeds.jpg")
                .rating(4.8)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("schedule", text("Delivers Tomorrow morning", "उद्या सकाळी डिलिव्हरी", "कल सुबह डिलीवरी"), Tone.NEUTRAL)
                .flashDeal()
                .barcode("8904567000010")
                .build();
    }

    private static Product batterySprayer(Shop shop) {
        return Product.builder(shop,
                        text("Kisan Shakti 16L Battery Sprayer", "किसान शक्ती 16 लि. बॅटरी स्प्रेयर", "किसान शक्ति 16 ली. बैटरी स्प्रेयर"),
                        text("Kisan Shakti 16L Sprayer", "किसान शक्ती 16 लि. स्प्रेयर", "किसान शक्ति 16 ली. स्प्रेयर"),
                        ProductCategory.TOOLS)
                .tag(text("1-Yr Motor Guarantee", "मोटरवर 1 वर्ष हमी", "मोटर पर 1 साल की गारंटी"), Tone.NEUTRAL)
                .description(text("12V/12Ah Dual Pump • Sprays 25-30 tanks", "12V/12Ah ड्युअल पंप • 25-30 टाक्या फवारणी",
                        "12V/12Ah डुअल पंप • 25-30 टंकी छिड़काव"))
                .price(2150, 2999)
                .image("/images/products/kisan-shakti-16l-sprayer.jpg")
                .imageBadge(text("28% OFF", "28% सूट", "28% छूट"), Tone.ERROR)
                .stockNote(text("Only 4 Left", "फक्त 4 शिल्लक", "सिर्फ़ 4 बचे"), Tone.ERROR)
                .footer("done_all", CASH_ON_DELIVERY, Tone.SUCCESS)
                .flashDeal()
                .barcode("8904567000027")
                .build();
    }

    private static Product mineralMix(Shop shop) {
        return Product.builder(shop,
                        text("Doodh Dhara Cattle Mineral Mix", "दूध धारा पशु खनिज मिश्रण", "दूध धारा पशु खनिज मिश्रण"),
                        text("Doodh Dhara 5kg", "दूध धारा 5 किलो", "दूध धारा 5 किलो"), ProductCategory.DAIRY)
                .tag(text("दूध उत्पादन वर्धक", "दूध उत्पादन वाढवते", "दूध उत्पादन वर्धक"), Tone.WARNING)
                .packSize(text("5 kg Bucket", "5 किलो बादली", "5 किलो बाल्टी"))
                .description(text("Chelated Minerals + Vitamins AD3E & Calcium", "चिलेटेड खनिजे + जीवनसत्त्वे AD3E व कॅल्शियम",
                        "चिलेटेड खनिज + विटामिन AD3E व कैल्शियम"))
                .price(450, 520)
                .image("/images/products/doodh-dhara-mineral-mix.jpg")
                .imageBadge(text("Vet Seal", "पशुवैद्य शिक्का", "पशु चिकित्सक मुहर"), Tone.SUCCESS)
                .stockNote(text("+10% Milk Fat", "+10% दुधाची फॅट", "+10% दूध फैट"), Tone.SUCCESS)
                .footer("health_and_safety", text("Dosage: 50g per day per cow", "मात्रा: प्रति गाय दररोज 50 ग्रॅम",
                        "खुराक: प्रति गाय रोज़ 50 ग्राम"), Tone.WARNING)
                .flashDeal()
                .barcode("8904567000034")
                .build();
    }

    private static Product hexaconazole(Shop shop) {
        return Product.builder(shop,
                        text("Tata Contaf Plus (Hexaconazole 5% SC)", "टाटा कॉन्टाफ प्लस (हेक्साकोनाझोल 5% SC)",
                                "टाटा कॉन्टाफ प्लस (हेक्साकोनाज़ोल 5% SC)"),
                        text("Tata Contaf Plus 500ml", "टाटा कॉन्टाफ प्लस 500 मि.लि.", "टाटा कॉन्टाफ प्लस 500 मि.ली."),
                        ProductCategory.CROP_MEDICINE)
                .tag(text("Systemic Fungicide", "आंतरप्रवाही बुरशीनाशक", "सिस्टमिक फफूंदनाशी"), Tone.NEUTRAL)
                .packSize(text("500 ml", "500 मि.लि.", "500 मि.ली."))
                .description(text("Controls Sheath Blight, Tikka disease & Rust", "शीथ ब्लाइट, टिक्का रोग व तांबेरा नियंत्रित करते",
                        "शीथ ब्लाइट, टिक्का रोग व रतुआ पर नियंत्रण"))
                .price(395, 460)
                .image("/images/products/tata-contaf-plus.jpg")
                .stockNote(text("Fast Dispatch", "लगेच रवाना", "तुरंत रवाना"), Tone.SUCCESS)
                .footer("verified_user", text("QR Batch Genuine Verified", "QR बॅच अस्सल असल्याची खात्री",
                        "QR बैच असली होने की पुष्टि"), Tone.NEUTRAL)
                .flashDeal()
                .barcode("8904567000041")
                .build();
    }

    private static Product mancozeb(Shop shop) {
        return Product.builder(shop,
                        text("Indofil M-45 Mancozeb 75% WP", "इंडोफिल M-45 मॅन्कोझेब 75% WP", "इंडोफिल M-45 मैन्कोज़ेब 75% WP"),
                        text("Mancozeb 500g", "मॅन्कोझेब 500 ग्रॅम", "मैन्कोज़ेब 500 ग्राम"), ProductCategory.CROP_MEDICINE)
                .tag(text("Contact Fungicide", "स्पर्शजन्य बुरशीनाशक", "संपर्क फफूंदनाशी"), Tone.NEUTRAL)
                .packSize(text("500 g", "500 ग्रॅम", "500 ग्राम"))
                .description(text("Broad-spectrum contact protection against Early & Late Blight",
                        "लवकर व उशिरा येणाऱ्या करप्यापासून व्यापक संरक्षण", "अगेती व पछेती झुलसा से व्यापक सुरक्षा"))
                .price(280, 320)
                .image(MANCOZEB_IMAGE)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("verified_user", text("Recommended for Plot A Rx", "प्लॉट A च्या उपचारासाठी शिफारस",
                        "प्लॉट A के इलाज के लिए अनुशंसित"), Tone.NEUTRAL)
                .barcode("8904567000058")
                .build();
    }

    private static Product blightRxBundle(Shop shop) {
        return Product.builder(shop,
                        text("Mancozeb 75% WP + Nozzle Kit", "मॅन्कोझेब 75% WP + नोझल किट", "मैन्कोज़ेब 75% WP + नोज़ल किट"),
                        text("Plot A Blight Rx Bundle", "प्लॉट A करपा उपचार संच", "प्लॉट A झुलसा उपचार बंडल"),
                        ProductCategory.CROP_MEDICINE)
                .tag(text("Doctor's Rx", "डॉक्टरांची शिफारस", "डॉक्टर की सलाह"), Tone.SUCCESS)
                .packSize(text("500 g + Nozzle", "500 ग्रॅम + नोझल", "500 ग्राम + नोज़ल"))
                .description(text("Indofil M-45 (500g) + Brass Mist Nozzle", "इंडोफिल M-45 (500 ग्रॅम) + पितळी फवारणी नोझल",
                        "इंडोफिल M-45 (500 ग्राम) + पीतल मिस्ट नोज़ल"))
                .price(420, 530)
                .image(MANCOZEB_IMAGE)
                .imageBadge(text("Rx", "Rx", "Rx"), Tone.SUCCESS)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("prescriptions", text("Matches your Plot A diagnosis", "तुमच्या प्लॉट A च्या निदानाशी जुळते",
                        "आपके प्लॉट A के निदान से मेल खाता है"), Tone.SUCCESS)
                .barcode("8904567000065")
                .build();
    }

    private static Product imidacloprid(Shop shop) {
        return Product.builder(shop,
                        text("Bayer Confidor (Imidacloprid 17.8% SL)", "बायर कॉन्फिडॉर (इमिडाक्लोप्रिड 17.8% SL)",
                                "बायर कॉन्फिडोर (इमिडाक्लोप्रिड 17.8% SL)"),
                        text("Confidor 100ml", "कॉन्फिडॉर 100 मि.लि.", "कॉन्फिडोर 100 मि.ली."), ProductCategory.CROP_MEDICINE)
                .tag(text("Systemic Insecticide", "आंतरप्रवाही कीटकनाशक", "सिस्टमिक कीटनाशक"), Tone.NEUTRAL)
                .packSize(text("100 ml", "100 मि.लि.", "100 मि.ली."))
                .description(text("Controls aphids, jassids & whiteflies on cotton and vegetables",
                        "कापूस व भाजीपाल्यावरील मावा, तुडतुडे व पांढरी माशी नियंत्रित करते",
                        "कपास व सब्ज़ियों पर माहू, जैसिड व सफ़ेद मक्खी पर नियंत्रण"))
                .price(260, 299)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("science", text("Use 0.5 ml per litre of water", "1 लिटर पाण्यात 0.5 मि.लि. वापरा",
                        "1 लीटर पानी में 0.5 मि.ली. डालें"), Tone.NEUTRAL)
                .barcode("8904567000072")
                .build();
    }

    private static Product neemOil(Shop shop) {
        return Product.builder(shop,
                        text("Neem Oil 10000 PPM Bio-Pesticide", "निंबोळी तेल 10000 PPM जैविक कीटकनाशक",
                                "नीम तेल 10000 PPM जैविक कीटनाशक"),
                        text("Neem Oil 1L", "निंबोळी तेल 1 लि.", "नीम तेल 1 ली."), ProductCategory.CROP_MEDICINE)
                .tag(text("Organic", "सेंद्रिय", "जैविक"), Tone.SUCCESS)
                .packSize(text("1 L", "1 लि.", "1 ली."))
                .description(text("Natural repellent for sucking pests; safe for bees when sprayed at dusk",
                        "रस शोषणाऱ्या किडींवर नैसर्गिक उपाय; संध्याकाळी फवारल्यास मधमाश्यांना सुरक्षित",
                        "रस चूसने वाले कीटों पर प्राकृतिक उपाय; शाम को छिड़कने पर मधुमक्खियों के लिए सुरक्षित"))
                .price(540, 620)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("eco", text("Approved for organic farms", "सेंद्रिय शेतीसाठी मान्य", "जैविक खेती के लिए मान्य"),
                        Tone.SUCCESS)
                .barcode("8904567000089")
                .build();
    }

    private static Product btCottonSeeds(Shop shop) {
        return Product.builder(shop,
                        text("Mahyco Bt Cotton Seeds (MRC 7351)", "महिको बीटी कापूस बियाणे (MRC 7351)",
                                "महिको बीटी कपास बीज (MRC 7351)"),
                        text("Bt Cotton 450g", "बीटी कापूस 450 ग्रॅम", "बीटी कपास 450 ग्राम"), ProductCategory.SEEDS)
                .tag(text("Bollworm Tolerant", "बोंडअळी सहनशील", "सुंडी सहनशील"), Tone.SUCCESS)
                .packSize(text("450 g Pkt", "450 ग्रॅम पाकीट", "450 ग्राम पैकेट"))
                .description(text("High-yield Bt hybrid for black cotton soil", "काळ्या जमिनीसाठी जास्त उत्पादन देणारे बीटी संकरित",
                        "काली मिट्टी के लिए ज़्यादा उपज वाला बीटी संकर"))
                .price(864)
                .stockNote(text("Only 9 Left", "फक्त 9 शिल्लक", "सिर्फ़ 9 बचे"), Tone.ERROR)
                .footer("schedule", text("Sow by 15 June", "15 जूनपर्यंत पेरणी करा", "15 जून तक बुवाई करें"), Tone.WARNING)
                .barcode("8904567000096")
                .build();
    }

    private static Product wheatSeeds(Shop shop) {
        return Product.builder(shop,
                        text("HD-2967 Wheat Seeds (Certified)", "HD-2967 गहू बियाणे (प्रमाणित)", "HD-2967 गेहूँ बीज (प्रमाणित)"),
                        text("HD-2967 Wheat 40kg", "HD-2967 गहू 40 किलो", "HD-2967 गेहूँ 40 किलो"), ProductCategory.SEEDS)
                .tag(text("ICAR Certified", "ICAR प्रमाणित", "ICAR प्रमाणित"), Tone.SUCCESS)
                .packSize(text("40 kg Bag", "40 किलो पोते", "40 किलो बोरी"))
                .description(text("Rust-resistant wheat for timely Rabi sowing", "वेळेवर रब्बी पेरणीसाठी तांबेरा प्रतिकारक गहू",
                        "समय पर रबी बुवाई के लिए रतुआ-रोधी गेहूँ"))
                .price(1850, 2100)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("local_shipping", text("Free delivery to your village", "तुमच्या गावापर्यंत मोफत डिलिव्हरी",
                        "आपके गाँव तक मुफ़्त डिलीवरी"), Tone.SUCCESS)
                .barcode("8904567000102")
                .build();
    }

    private static Product onionSeeds(Shop shop) {
        return Product.builder(shop,
                        text("Onion Seeds N-53 (Red)", "कांदा बियाणे N-53 (लाल)", "प्याज़ बीज N-53 (लाल)"),
                        text("Onion N-53 500g", "कांदा N-53 500 ग्रॅम", "प्याज़ N-53 500 ग्राम"), ProductCategory.SEEDS)
                .tag(text("Kharif Onion", "खरीप कांदा", "खरीफ़ प्याज़"), Tone.NEUTRAL)
                .packSize(text("500 g", "500 ग्रॅम", "500 ग्राम"))
                .description(text("Dark red bulbs with a long storage life", "जास्त काळ टिकणारे गडद लाल कांदे",
                        "लंबे समय तक टिकने वाले गहरे लाल प्याज़"))
                .price(1200, 1350)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("verified_user", text("Germination tested 80%+", "उगवणक्षमता 80%+ तपासलेली",
                        "अंकुरण 80%+ जाँचा हुआ"), Tone.NEUTRAL)
                .barcode("8904567000119")
                .build();
    }

    private static Product handSprayer(Shop shop) {
        return Product.builder(shop,
                        text("Knapsack Hand Sprayer 16L", "पाठीवरचा हात पंप 16 लि.", "पीठ पर टाँगने वाला हैंड स्प्रेयर 16 ली."),
                        text("Hand Sprayer 16L", "हात पंप 16 लि.", "हैंड स्प्रेयर 16 ली."), ProductCategory.TOOLS)
                .tag(text("Heavy Duty", "मजबूत", "मज़बूत"), Tone.NEUTRAL)
                .description(text("Brass lance & adjustable nozzle; no battery needed",
                        "पितळी नळी व बदलता येणारे नोझल; बॅटरीची गरज नाही", "पीतल की नली व बदलने योग्य नोज़ल; बैटरी की ज़रूरत नहीं"))
                .price(1150, 1499)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("done_all", CASH_ON_DELIVERY, Tone.SUCCESS)
                .barcode("8904567000126")
                .build();
    }

    private static Product dripKit(Shop shop) {
        return Product.builder(shop,
                        text("Drip Irrigation Kit (1 Acre)", "ठिबक सिंचन किट (1 एकर)", "ड्रिप सिंचाई किट (1 एकड़)"),
                        text("Drip Kit 1 Acre", "ठिबक किट 1 एकर", "ड्रिप किट 1 एकड़"), ProductCategory.TOOLS)
                .tag(text("Water Saver", "पाण्याची बचत", "पानी की बचत"), Tone.SUCCESS)
                .description(text("16 mm laterals, filters & fittings for one acre",
                        "एका एकरासाठी 16 मिमी लॅटरल, फिल्टर व फिटिंग्ज", "एक एकड़ के लिए 16 मिमी लेटरल, फ़िल्टर व फ़िटिंग"))
                .price(18500, 22000)
                .stockNote(text("Ships in 3 days", "3 दिवसांत रवाना", "3 दिन में रवाना"), Tone.WARNING)
                .footer("engineering", text("Free installation guidance", "बसवण्यासाठी मोफत मार्गदर्शन",
                        "लगाने के लिए मुफ़्त मार्गदर्शन"), Tone.NEUTRAL)
                .barcode("8904567000133")
                .build();
    }

    /** Sold out: listed, but it cannot be added to a cart until the depot restocks it. */
    private static Product secateurs(Shop shop) {
        return Product.builder(shop,
                        text("Pruning Secateurs (Steel)", "छाटणी कात्री (स्टील)", "कटाई कैंची (स्टील)"),
                        text("Secateurs", "छाटणी कात्री", "कटाई कैंची"), ProductCategory.TOOLS)
                .description(text("Rust-proof blades for grapes, pomegranate & roses",
                        "द्राक्ष, डाळिंब व गुलाबासाठी गंजरोधक पाती", "अंगूर, अनार व गुलाब के लिए ज़ंग-रोधी ब्लेड"))
                .price(349, 450)
                .stockNote(text("Out of Stock", "स्टॉक संपला", "स्टॉक ख़त्म"), Tone.ERROR)
                .footer("notifications", text("Restocking soon", "लवकरच पुन्हा उपलब्ध", "जल्द फिर उपलब्ध"), Tone.NEUTRAL)
                .outOfStock()
                .barcode("8904567000140")
                .build();
    }

    private static Product calciumSupplement(Shop shop) {
        return Product.builder(shop,
                        text("Calcium Liquid Supplement for Cattle", "जनावरांसाठी द्रव कॅल्शियम", "पशुओं के लिए तरल कैल्शियम"),
                        text("Calcium Gel 1L", "कॅल्शियम जेल 1 लि.", "कैल्शियम जेल 1 ली."), ProductCategory.DAIRY)
                .tag(text("Vet Recommended", "पशुवैद्यांची शिफारस", "पशु चिकित्सक की सलाह"), Tone.SUCCESS)
                .packSize(text("1 L", "1 लि.", "1 ली."))
                .description(text("Prevents milk fever after calving", "विल्यानंतरचा दुग्धज्वर टाळते", "ब्याने के बाद दुग्ध ज्वर से बचाव"))
                .price(380, 420)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("health_and_safety", text("Dose: 100 ml per day", "मात्रा: दररोज 100 मि.लि.", "खुराक: रोज़ 100 मि.ली."),
                        Tone.WARNING)
                .barcode("8904567000157")
                .build();
    }

    private static Product cattleFeed(Shop shop) {
        return Product.builder(shop,
                        text("Cattle Feed Pellets (Sugras)", "पशुखाद्य गोळ्या (सुग्रास)", "पशु आहार पेलेट (सुग्रास)"),
                        text("Cattle Feed 50kg", "पशुखाद्य 50 किलो", "पशु आहार 50 किलो"), ProductCategory.DAIRY)
                .tag(text("High Protein", "जास्त प्रथिने", "ज़्यादा प्रोटीन"), Tone.NEUTRAL)
                .packSize(text("50 kg Bag", "50 किलो पोते", "50 किलो बोरी"))
                .description(text("22% protein pellets for a higher milk yield", "जास्त दूध उत्पादनासाठी 22% प्रथिनयुक्त गोळ्या",
                        "ज़्यादा दूध के लिए 22% प्रोटीन वाले पेलेट"))
                .price(1450, 1600)
                .stockNote(IN_STOCK, Tone.SUCCESS)
                .footer("local_shipping", text("Delivered with your next order", "पुढच्या ऑर्डरसोबत डिलिव्हरी",
                        "अगले ऑर्डर के साथ डिलीवरी"), Tone.NEUTRAL)
                .barcode("8904567000164")
                .build();
    }

    private static LocalizedText text(String en, String mr, String hi) {
        return LocalizedText.of(en, mr, hi);
    }
}
