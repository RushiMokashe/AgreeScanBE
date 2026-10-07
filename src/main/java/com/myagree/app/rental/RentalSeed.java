package com.myagree.app.rental;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.myagree.app.account.Account;
import com.myagree.app.account.AccountService;
import com.myagree.app.account.NewAccount;
import com.myagree.app.common.i18n.Language;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.common.security.Role;
import com.myagree.app.farmer.Farmer;
import com.myagree.app.seed.AccountSeed;
import com.myagree.app.seed.AccountSeed.DemoAccounts;
import com.myagree.app.seed.DemoSeed;
import com.myagree.app.seed.FarmerSeed;
import com.myagree.app.seed.SeedContext;

/**
 * The rentals demo data: Solapur APMC Hub exactly as the Stitch design shows it (the Bolero "Mandi Harvest Express"
 * spotlight and four vehicles), two more hubs with vehicles of their own, a vehicle-owner profile for every owner
 * account, and three of Rishikesh's bookings at different stages of the lifecycle. Every text is in English, Marathi
 * and Hindi; names of people and businesses stay as written.
 */
@Component
class RentalSeed implements DemoSeed {

    /** Owners of the vehicles at the two extra hubs; their accounts are created here. */
    private static final String EXTRA_OWNER_PASSWORD = "Owner@123";

    private final RentalHubRepository hubRepository;
    private final VehicleOwnerRepository ownerRepository;
    private final RentalListingRepository listingRepository;
    private final RentalSpotlightRepository spotlightRepository;
    private final RentalBookingRepository bookingRepository;
    private final AccountService accountService;
    private final Clock clock;

    RentalSeed(RentalHubRepository hubRepository, VehicleOwnerRepository ownerRepository,
               RentalListingRepository listingRepository, RentalSpotlightRepository spotlightRepository,
               RentalBookingRepository bookingRepository, AccountService accountService, Clock clock) {
        this.hubRepository = hubRepository;
        this.ownerRepository = ownerRepository;
        this.listingRepository = listingRepository;
        this.spotlightRepository = spotlightRepository;
        this.bookingRepository = bookingRepository;
        this.accountService = accountService;
        this.clock = clock;
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public void seed(SeedContext context) {
        DemoAccounts accounts = context.get(AccountSeed.ACCOUNTS);
        RentalHub solapur = hubRepository.save(new RentalHub(
                text("Solapur APMC Hub", "सोलापूर APMC हब", "सोलापुर APMC हब"), 12,
                text("Karmala, Kem & Kurduwadi routes", "करमाळा, केम व कुर्डुवाडी मार्ग", "करमाला, केम और कुर्डुवाडी मार्ग")));
        RentalHub pune = hubRepository.save(new RentalHub(
                text("Pune Market Yard Hub", "पुणे मार्केट यार्ड हब", "पुणे मार्केट यार्ड हब"), 15,
                text("Gultekdi, Hadapsar & Saswad routes", "गुलटेकडी, हडपसर व सासवड मार्ग", "गुलटेकड़ी, हडपसर और सासवड मार्ग")));
        RentalHub lasalgaon = hubRepository.save(new RentalHub(
                text("Lasalgaon (Nashik) Hub", "लासलगाव (नाशिक) हब", "लासलगाँव (नाशिक) हब"), 18,
                text("Niphad, Pimpalgaon & Yeola routes", "निफाड, पिंपळगाव व येवला मार्ग", "निफाड, पिंपलगाँव और येवला मार्ग")));

        VehicleOwner rameshwar = owner(accounts.rameshwarPatil(), null, solapur);
        VehicleOwner shivraj = owner(accounts.shivrajAgroService(), "Shivraj Agro Service", solapur);
        VehicleOwner vinod = owner(accounts.vinodShinde(), null, solapur);
        VehicleOwner balwant = owner(accounts.balwantTransportFleet(), "Balwant Transport Fleet", solapur);
        VehicleOwner agriscanLogistics = owner(accounts.agriscanLogistics(), "AgriScan Logistics", solapur);

        RentalListing boleroPickup = listingRepository.save(boleroPickup(agriscanLogistics, solapur));
        spotlightRepository.save(new RentalSpotlight(boleroPickup,
                text("Mandi Harvest Express • ताजी मंडी रवानगी", "मंडी हार्वेस्ट एक्सप्रेस • ताजी मंडी रवानगी",
                        "मंडी हार्वेस्ट एक्सप्रेस • ताज़ा मंडी रवानगी"),
                "/images/rentals/bolero-pickup.jpg", 25, List.of(
                        new RentalPerk("verified_user", text("Zero Spill", "शून्य सांडणी", "शून्य गिरावट"),
                                text("Tarpaulin Tied", "ताडपत्री बांधलेली", "तिरपाल बंधा हुआ")),
                        new RentalPerk("scale", text("Dharmakanta", "धर्मकाटा", "धर्मकांटा"),
                                text("Gross Slip Shared", "वजन पावती दिली जाते", "वज़न पर्ची साझा")),
                        new RentalPerk("group", text("1 Loader Free", "1 हमाल मोफत", "1 लोडर मुफ़्त"),
                                text("Farm crate lifting", "शेतातून क्रेट उचलणे", "खेत से क्रेट उठाना")))));
        RentalListing rotavator = rotavatorTractor(rameshwar, solapur);
        RentalListing tataAce = tataAce(vinod, solapur);
        listingRepository.saveAll(List.of(rotavator, heavyTractor(shivraj, solapur), tataAce,
                boleroMaxiTruck(balwant, solapur)));

        VehicleOwner ganesh = owner(extraOwnerAccount("Ganesh Kale", "9000000011"), null, pune);
        VehicleOwner pawarTransport = owner(extraOwnerAccount("Pawar Transport", "9000000012"), "Pawar Transport", pune);
        VehicleOwner nitin = owner(extraOwnerAccount("Nitin Aher", "9000000013"), null, lasalgaon);
        VehicleOwner onionCarriers = owner(extraOwnerAccount("Lasalgaon Onion Carriers", "9000000014"),
                "Lasalgaon Onion Carriers", lasalgaon);
        listingRepository.saveAll(List.of(sonalikaTractor(ganesh, pune), eicherTempo(pawarTransport, pune),
                swarajRotavator(nitin, lasalgaon), onionCarrier(onionCarriers, lasalgaon), dayTractor(nitin, lasalgaon)));

        Farmer rishikesh = context.get(FarmerSeed.FARMERS).rishikesh();
        seedBookings(new BookingFarmer(rishikesh.getId(), rishikesh.getUserId(), rishikesh.getName(),
                accounts.rishikesh().phone(), rishikesh.getLocation()), rotavator, tataAce);
    }

    /**
     * Rishikesh's bookings: a request Rameshwar has still to answer, a Tata Ace trip Vinod accepted, and a rotavator
     * job completed last week and settled with the owner in cash.
     */
    private void seedBookings(BookingFarmer rishikesh, RentalListing rotavator, RentalListing tataAce) {
        Instant now = clock.instant();
        RentalBooking pastJob = rotavator.book(rishikesh, "Slot: 07:00 AM, Monday", 2, now.minus(Duration.ofDays(8)));
        pastJob.move(BookingTransition.ACCEPT, null, now.minus(Duration.ofDays(8)).plus(Duration.ofMinutes(20)));
        pastJob.move(BookingTransition.START, null, now.minus(Duration.ofDays(7)).minus(Duration.ofHours(3)));
        pastJob.move(BookingTransition.COMPLETE, null, now.minus(Duration.ofDays(7)));

        RentalBooking mandiTrip = tataAce.book(rishikesh, "Tomorrow 6:00 AM", 20, now.minus(Duration.ofDays(1)));
        mandiTrip.move(BookingTransition.ACCEPT, null, now.minus(Duration.ofHours(23)));

        RentalBooking request = rotavator.book(rishikesh, "Slot: 02:00 PM Today", 3, now.minus(Duration.ofMinutes(40)));
        bookingRepository.saveAll(List.of(pastJob, mandiTrip, request));
    }

    private VehicleOwner owner(Account account, @Nullable String businessName, RentalHub hub) {
        VehicleOwner owner = ownerRepository.save(new VehicleOwner(account.id(), account.name(), businessName,
                account.phone(), hub));
        accountService.linkOwnerProfile(account.id(), owner.getId());
        return owner;
    }

    private Account extraOwnerAccount(String name, String phone) {
        return accountService.create(new NewAccount(name, phone, null, Set.of(Role.VEHICLE_OWNER), EXTRA_OWNER_PASSWORD,
                Language.DEFAULT));
    }

    /** Backs the spotlight offer; booked through the spotlight card, never listed in the grid. */
    private static RentalListing boleroPickup(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.TRANSPORT, hub,
                        text("Mahindra Bolero Pickup", "महिंद्रा बोलेरो पिकअप", "महिंद्रा बोलेरो पिकअप"),
                        text("Rated 1.7 Tonne • 65–75 Vegetable Crates", "1.7 टन क्षमता • 65–75 भाजीपाला क्रेट्स",
                                "1.7 टन क्षमता • 65–75 सब्ज़ी क्रेट"),
                        "local_shipping", 22, RateUnit.KM,
                        text("Min ₹450 Base", "किमान ₹450 बेस भाडे", "न्यूनतम ₹450 बेस किराया"),
                        text("Dispatches in 25 mins", "25 मिनिटांत रवाना", "25 मिनट में रवाना"),
                        true, List.of(), List.of()))
                .baseFare(450)
                .highlightRateNote()
                .highlightAvailability()
                .operatorStats(text("450+ Mandi runs • 4.8 ★", "450+ मंडी फेऱ्या • 4.8 ★", "450+ मंडी फेरे • 4.8 ★"))
                .labels(callDriver(), text("Book Tempo", "टेम्पो बुक करा", "टेम्पो बुक करें"), "arrow_forward")
                .build();
    }

    private static RentalListing rotavatorTractor(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.MACHINERY, hub,
                        text("Mahindra 575 DI (45 HP) + Rotavator", "महिंद्रा 575 DI (45 HP) + रोटाव्हेटर",
                                "महिंद्रा 575 DI (45 HP) + रोटावेटर"),
                        text("Specialized for Seedbed Prep, Onion & Wheat Tillage",
                                "बियाण्यासाठी वाफे तयार करणे, कांदा व गहू मशागतीसाठी खास",
                                "बीज क्यारी तैयारी, प्याज़ व गेहूँ की जुताई के लिए खास"),
                        null, 650, RateUnit.HOUR,
                        text("With Diesel + Driver", "डिझेल + ड्रायव्हरसह", "डीज़ल + ड्राइवर सहित"),
                        text("Slot: 02:00 PM Today", "स्लॉट: आज दुपारी 02:00", "स्लॉट: आज दोपहर 02:00"),
                        true,
                        List.of(new RentalSpec(text("6 Feet", "6 फूट", "6 फ़ीट"),
                                        text("Rotavator Width", "रोटाव्हेटर रुंदी", "रोटावेटर चौड़ाई")),
                                new RentalSpec(text("Dual Clutch", "ड्युअल क्लच", "डुअल क्लच"),
                                        text("Fine Soil Grinding", "माती बारीक भुसभुशीत", "मिट्टी की बारीक पिसाई")),
                                new RentalSpec(text("Pay Later", "नंतर पैसे द्या", "बाद में भुगतान"),
                                        text("Post Tillage Check", "मशागतीनंतर तपासणी", "जुताई के बाद जाँच"))),
                        List.of()))
                .photo("/images/rentals/mahindra-575-rotavator.jpg")
                .badge(text("Verified Field Tested", "शेतात तपासलेले", "खेत में जाँचा गया"), "verified")
                .location(2.4, text("Kem Shivar", "केम शिवार", "केम शिवार"))
                .highlightRateNote()
                .highlightAvailability()
                .operatorStats(text("120+ acres tilled • 4.9 ★★★★★", "120+ एकर मशागत • 4.9 ★★★★★",
                        "120+ एकड़ जुताई • 4.9 ★★★★★"))
                .labels(text("Call Rameshwar", "रामेश्वर यांना कॉल करा", "रामेश्वर को कॉल करें"), bookSlot(),
                        "calendar_month")
                .build();
    }

    private static RentalListing heavyTractor(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.MACHINERY, hub,
                        text("John Deere 5050 D (50 HP)", "जॉन डियर 5050 D (50 HP)", "जॉन डियर 5050 D (50 HP)"),
                        text("Includes 9-Tyne Cultivator & Double Axle Trolley",
                                "9-फणी कल्टिव्हेटर व डबल ॲक्सल ट्रॉलीसह", "9-टाइन कल्टीवेटर और डबल एक्सल ट्रॉली सहित"),
                        null, 700, RateUnit.HOUR,
                        text("or ₹2,800/Day", "किंवा ₹2,800/दिवस", "या ₹2,800/दिन"),
                        text("GPS Enabled", "GPS सुविधा", "GPS सुविधा"),
                        true, List.of(), List.of()))
                .photo("/images/rentals/john-deere-5050d.jpg")
                .badge(text("Heavy Soil Certified", "भारी जमिनीसाठी प्रमाणित", "भारी मिट्टी के लिए प्रमाणित"),
                        "check_circle")
                .location(4.1, text("Karmala Taluka", "करमाळा तालुका", "करमाला तालुका"))
                .operatorStats(text("Commercial Fleet • 4.8 ★★★★★", "व्यावसायिक ताफा • 4.8 ★★★★★",
                        "व्यावसायिक बेड़ा • 4.8 ★★★★★"))
                .labels(text("Contact Fleet", "ताफ्याशी संपर्क करा", "बेड़े से संपर्क करें"), bookSlot(), "event_available")
                .build();
    }

    private static RentalListing tataAce(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.TRANSPORT, hub,
                        text("Tata Ace Gold (छोटा हाथी)", "टाटा एस गोल्ड (छोटा हाथी)", "टाटा ऐस गोल्ड (छोटा हाथी)"),
                        text("Capacity: 750 kg (25–30 tomato crates)", "क्षमता: 750 किलो (25–30 टोमॅटो क्रेट्स)",
                                "क्षमता: 750 किलो (25–30 टमाटर क्रेट)"),
                        "local_shipping", 18, RateUnit.KM,
                        text("Min ₹350 Base", "किमान ₹350 बेस भाडे", "न्यूनतम ₹350 बेस किराया"),
                        text("At Farm in 20m", "20 मिनिटांत शेतावर", "20 मिनट में खेत पर"),
                        true, List.of(),
                        List.of(new RentalFeature("badge", text("Solapur APMC Direct Entry Pass",
                                        "सोलापूर APMC थेट प्रवेश पास", "सोलापुर APMC सीधा प्रवेश पास")),
                                new RentalFeature("layers", text("Crate Strapping Ropes Included",
                                        "क्रेट बांधण्याचे दोर सोबत", "क्रेट बाँधने की रस्सियाँ शामिल")))))
                .highlightRateNote()
                .highlightAvailability()
                .operatorStats(text("320+ Mandi runs • 4.9 ★", "320+ मंडी फेऱ्या • 4.9 ★", "320+ मंडी फेरे • 4.9 ★"))
                .labels(callDriver(), text("Dispatch to Farm", "शेतावर पाठवा", "खेत पर भेजें"), "near_me")
                .build();
    }

    private static RentalListing boleroMaxiTruck(VehicleOwner owner, RentalHub hub) {
        LocalizedText depot = text("Karmala Depot • 4.7 ★", "करमाळा डेपो • 4.7 ★", "करमाला डिपो • 4.7 ★");
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.TRANSPORT, hub,
                        text("Mahindra Bolero Maxi Truck", "महिंद्रा बोलेरो मॅक्सी ट्रक", "महिंद्रा बोलेरो मैक्सी ट्रक"),
                        text("Capacity: 1.7 Tonne (70–80 Crates)", "क्षमता: 1.7 टन (70–80 क्रेट्स)",
                                "क्षमता: 1.7 टन (70–80 क्रेट)"),
                        "rv_hookup", 24, RateUnit.KM, depot,
                        text("Available Tomorrow", "उद्या उपलब्ध", "कल उपलब्ध"),
                        true, List.of(),
                        List.of(new RentalFeature("alarm", text("4:00 AM Dawn Auction Slot", "पहाटे 4:00 लिलाव स्लॉट",
                                        "सुबह 4:00 नीलामी स्लॉट")),
                                new RentalFeature("water_drop", text("Monsoon Waterproof Canopy", "पावसाळी जलरोधक छत",
                                        "मानसून वाटरप्रूफ छत")))))
                .operatorStats(depot)
                .labels(text("Contact Owner", "मालकाशी संपर्क करा", "मालिक से संपर्क करें"),
                        text("Reserve 4 AM Run", "पहाटे 4 ची फेरी राखून ठेवा", "सुबह 4 बजे की फेरी आरक्षित करें"),
                        "schedule")
                .build();
    }

    private static RentalListing sonalikaTractor(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.MACHINERY, hub,
                        text("Sonalika DI 745 III (50 HP) + Cultivator", "सोनालिका DI 745 III (50 HP) + कल्टिव्हेटर",
                                "सोनालीका DI 745 III (50 HP) + कल्टीवेटर"),
                        text("Deep tillage for sugarcane & vegetable plots", "ऊस व भाजीपाला प्लॉटसाठी खोल मशागत",
                                "गन्ना और सब्ज़ी के प्लॉट के लिए गहरी जुताई"),
                        null, 750, RateUnit.HOUR,
                        text("With Diesel + Driver", "डिझेल + ड्रायव्हरसह", "डीज़ल + ड्राइवर सहित"),
                        text("Slot: 09:00 AM Tomorrow", "स्लॉट: उद्या सकाळी 09:00", "स्लॉट: कल सुबह 09:00"),
                        true,
                        List.of(new RentalSpec(text("9 Tyne", "9 फणी", "9 टाइन"),
                                text("Cultivator", "कल्टिव्हेटर", "कल्टीवेटर"))),
                        List.of()))
                .location(3.2, text("Hadapsar", "हडपसर", "हडपसर"))
                .operatorStats(text("80+ acres tilled • 4.7 ★", "80+ एकर मशागत • 4.7 ★", "80+ एकड़ जुताई • 4.7 ★"))
                .build();
    }

    private static RentalListing eicherTempo(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.TRANSPORT, hub,
                        text("Eicher Pro 2049 Tempo", "आयशर प्रो 2049 टेम्पो", "आयशर प्रो 2049 टेम्पो"),
                        text("Capacity: 2 Tonne (90–100 crates) • Gultekdi entry",
                                "क्षमता: 2 टन (90–100 क्रेट्स) • गुलटेकडी प्रवेश",
                                "क्षमता: 2 टन (90–100 क्रेट) • गुलटेकड़ी प्रवेश"),
                        null, 26, RateUnit.KM, null,
                        text("At Farm in 40m", "40 मिनिटांत शेतावर", "40 मिनट में खेत पर"),
                        true, List.of(),
                        List.of(new RentalFeature("badge", text("Pune Market Yard Gate Pass", "पुणे मार्केट यार्ड गेट पास",
                                "पुणे मार्केट यार्ड गेट पास")))))
                .operatorStats(text("200+ Mandi runs • 4.6 ★", "200+ मंडी फेऱ्या • 4.6 ★", "200+ मंडी फेरे • 4.6 ★"))
                .build();
    }

    private static RentalListing swarajRotavator(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.MACHINERY, hub,
                        text("Swaraj 744 FE (48 HP) + Rotavator", "स्वराज 744 FE (48 HP) + रोटाव्हेटर",
                                "स्वराज 744 FE (48 HP) + रोटावेटर"),
                        text("Onion bed preparation & ridging", "कांद्यासाठी वाफे तयार करणे व सरी पाडणे",
                                "प्याज़ की क्यारी बनाना और मेड़ बनाना"),
                        null, 680, RateUnit.HOUR,
                        text("With Diesel + Driver", "डिझेल + ड्रायव्हरसह", "डीज़ल + ड्राइवर सहित"),
                        text("Slot: 07:00 AM Tomorrow", "स्लॉट: उद्या सकाळी 07:00", "स्लॉट: कल सुबह 07:00"),
                        true, List.of(), List.of()))
                .location(5.0, text("Niphad", "निफाड", "निफाड"))
                .operatorStats(text("150+ acres tilled • 4.8 ★", "150+ एकर मशागत • 4.8 ★", "150+ एकड़ जुताई • 4.8 ★"))
                .build();
    }

    private static RentalListing onionCarrier(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.TRANSPORT, hub,
                        text("Tata 407 Onion Carrier", "टाटा 407 कांदा वाहक", "टाटा 407 प्याज़ वाहक"),
                        text("Capacity: 2.5 Tonne • Ventilated onion bags", "क्षमता: 2.5 टन • हवेशीर कांदा पिशव्या",
                                "क्षमता: 2.5 टन • हवादार प्याज़ बोरियाँ"),
                        "local_shipping", 28, RateUnit.KM, null,
                        text("Available Today", "आज उपलब्ध", "आज उपलब्ध"),
                        true, List.of(), List.of()))
                .highlightAvailability()
                .build();
    }

    /** Listed but switched off by its owner: farmers never see it. */
    private static RentalListing dayTractor(VehicleOwner owner, RentalHub hub) {
        return RentalListing.builder(owner, new ListingDetails(RentalCategory.MACHINERY, hub,
                        text("Swaraj 735 XT (40 HP) Day Hire", "स्वराज 735 XT (40 HP) दिवसभर भाड्याने",
                                "स्वराज 735 XT (40 HP) दिनभर किराये पर"),
                        text("Full-day hire for spraying & haulage", "फवारणी व वाहतुकीसाठी दिवसभर भाड्याने",
                                "छिड़काव और ढुलाई के लिए पूरे दिन किराये पर"),
                        null, 2600, RateUnit.DAY, null,
                        text("Back next week", "पुढच्या आठवड्यात उपलब्ध", "अगले सप्ताह उपलब्ध"),
                        false, List.of(), List.of()))
                .build();
    }

    private static LocalizedText callDriver() {
        return text("Call Driver", "ड्रायव्हरला कॉल करा", "ड्राइवर को कॉल करें");
    }

    private static LocalizedText bookSlot() {
        return text("Book Slot", "स्लॉट बुक करा", "स्लॉट बुक करें");
    }

    private static LocalizedText text(String en, String mr, String hi) {
        return LocalizedText.of(en, mr, hi);
    }
}
