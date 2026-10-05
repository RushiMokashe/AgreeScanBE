package com.myagree.app.seed;

import java.util.List;

import org.springframework.stereotype.Component;

import com.myagree.app.rental.RateUnit;
import com.myagree.app.rental.RentalCategory;
import com.myagree.app.rental.RentalFeature;
import com.myagree.app.rental.RentalHub;
import com.myagree.app.rental.RentalHubRepository;
import com.myagree.app.rental.RentalListing;
import com.myagree.app.rental.RentalListingRepository;
import com.myagree.app.rental.RentalOperator;
import com.myagree.app.rental.RentalPerk;
import com.myagree.app.rental.RentalSpec;
import com.myagree.app.rental.RentalSpotlight;
import com.myagree.app.rental.RentalSpotlightRepository;

/** The farm-rentals screen: hub, the Bolero "Mandi Harvest Express" spotlight and four listings. */
@Component
class RentalSeed implements DemoSeed {

    private final RentalHubRepository hubRepository;
    private final RentalListingRepository listingRepository;
    private final RentalSpotlightRepository spotlightRepository;

    RentalSeed(RentalHubRepository hubRepository, RentalListingRepository listingRepository,
               RentalSpotlightRepository spotlightRepository) {
        this.hubRepository = hubRepository;
        this.listingRepository = listingRepository;
        this.spotlightRepository = spotlightRepository;
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public void seed(SeedContext context) {
        hubRepository.save(new RentalHub("Solapur APMC Hub", 12, "Karmala, Kem & Kurduwadi routes", 14));
        RentalListing boleroPickup = listingRepository.save(boleroPickup());
        spotlightRepository.save(new RentalSpotlight(boleroPickup, "Mandi Harvest Express • ताजी मंडी रवानगी",
                "/images/rentals/bolero-pickup.jpg", 450, 25, List.of(
                        new RentalPerk("verified_user", "Zero Spill", "Tarpaulin Tied"),
                        new RentalPerk("scale", "Dharmakanta", "Gross Slip Shared"),
                        new RentalPerk("group", "1 Loader Free", "Farm crate lifting"))));
        listingRepository.saveAll(List.of(rotavatorTractor(), heavyTractor(), tataAce(), boleroMaxiTruck()));
    }

    /** Backs the spotlight offer; booked through the spotlight card, never listed in the grid. */
    private static RentalListing boleroPickup() {
        return RentalListing.builder(RentalCategory.TRANSPORT, "Mahindra Bolero Pickup", "Rated 1.7 Tonne • 65–75 Vegetable Crates")
                .photo("/images/rentals/bolero-pickup.jpg")
                .icon("local_shipping")
                .rate(22, RateUnit.KM)
                .rateNote("Min ₹450 Base", true)
                .operator(new RentalOperator("Sachin Jadhav", "SJ", "450+ Mandi runs • 4.8 ★"))
                .availability("Dispatches in 25 mins", true)
                .contact("+919850011223", "Call Driver")
                .booking("Book Tempo", "arrow_forward")
                .build();
    }

    private static RentalListing rotavatorTractor() {
        return RentalListing.builder(RentalCategory.MACHINERY, "Mahindra 575 DI (45 HP) + Rotavator",
                        "Specialized for Seedbed Prep, Onion & Wheat Tillage")
                .photo("/images/rentals/mahindra-575-rotavator.jpg")
                .badge("Verified Field Tested", "verified")
                .location(2.4, "Kem Shivar")
                .rate(650, RateUnit.HOUR)
                .rateNote("With Diesel + Driver", true)
                .operator(new RentalOperator("Rameshwar Patil", "RP", "120+ acres tilled • 4.9 ★★★★★"))
                .availability("Slot: 02:00 PM Today", true)
                .specs(List.of(
                        new RentalSpec("6 Feet", "Rotavator Width"),
                        new RentalSpec("Dual Clutch", "Fine Soil Grinding"),
                        new RentalSpec("Pay Later", "Post Tillage Check")))
                .contact("+919800012345", "Call Rameshwar")
                .booking("Book Slot", "calendar_month")
                .build();
    }

    private static RentalListing heavyTractor() {
        return RentalListing.builder(RentalCategory.MACHINERY, "John Deere 5050 D (50 HP)",
                        "Includes 9-Tyne Cultivator & Double Axle Trolley")
                .photo("/images/rentals/john-deere-5050d.jpg")
                .badge("Heavy Soil Certified", "check_circle")
                .location(4.1, "Karmala Taluka")
                .rate(700, RateUnit.HOUR)
                .rateNote("or ₹2,800/Day", false)
                .operator(new RentalOperator("Shivraj Agro Service", "SA", "Commercial Fleet • 4.8 ★★★★★"))
                .availability("GPS Enabled", false)
                .contact("+919800098765", "Contact Fleet")
                .booking("Book Slot", "event_available")
                .build();
    }

    private static RentalListing tataAce() {
        return RentalListing.builder(RentalCategory.TRANSPORT, "Tata Ace Gold (छोटा हाथी)",
                        "Capacity: 750 kg (25–30 tomato crates)")
                .icon("local_shipping")
                .rate(18, RateUnit.KM)
                .rateNote("Min ₹350 Base", true)
                .operator(new RentalOperator("Vinod Shinde", "VS", "320+ Mandi runs • 4.9 ★"))
                .availability("At Farm in 20m", true)
                .features(List.of(
                        new RentalFeature("badge", "Solapur APMC Direct Entry Pass"),
                        new RentalFeature("layers", "Crate Strapping Ropes Included")))
                .contact("+919822233445", "Call Driver")
                .booking("Dispatch to Farm", "near_me")
                .build();
    }

    private static RentalListing boleroMaxiTruck() {
        return RentalListing.builder(RentalCategory.TRANSPORT, "Mahindra Bolero Maxi Truck",
                        "Capacity: 1.7 Tonne (70–80 Crates)")
                .icon("rv_hookup")
                .rate(24, RateUnit.KM)
                .rateNote("Karmala Depot • 4.7 ★", false)
                .operator(new RentalOperator("Balwant Transport Fleet", "BT", "Karmala Depot • 4.7 ★"))
                .availability("Available Tomorrow", false)
                .features(List.of(
                        new RentalFeature("alarm", "4:00 AM Dawn Auction Slot"),
                        new RentalFeature("water_drop", "Monsoon Waterproof Canopy")))
                .contact("+919877766554", "Contact Owner")
                .booking("Reserve 4 AM Run", "schedule")
                .build();
    }
}
