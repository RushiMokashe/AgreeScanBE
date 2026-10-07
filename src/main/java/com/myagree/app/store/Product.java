package com.myagree.app.store;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.EmbeddedColumnNaming;
import org.jspecify.annotations.Nullable;

import com.myagree.app.common.Tone;
import com.myagree.app.common.i18n.LocalizedText;
import com.myagree.app.common.media.StoredPhoto;

/**
 * An item sold by the agro depot, together with the texts its catalogue card shows in English, Marathi and Hindi.
 * Prices are whole rupees. The admin portal changes price, MRP, flash-deal listing and stock; everything else is
 * catalogue data.
 */
@Entity
public class Product {

    /** Highest price or MRP the admin and shop portals accept, in rupees. */
    public static final int MAX_PRICE = 1_000_000;

    /** What a product a shopkeeper lists shows under its price and at its foot (seeded products have their own). */
    private static final LocalizedText LISTED_STOCK_NOTE = LocalizedText.of("In Stock", "स्टॉकमध्ये", "स्टॉक में");
    private static final String LISTED_FOOTER_ICON = "payments";
    private static final LocalizedText LISTED_FOOTER = LocalizedText.of("Cash on delivery or pay online",
            "डिलिव्हरीवेळी रोख किंवा ऑनलाइन पेमेंट", "डिलीवरी पर नकद या ऑनलाइन भुगतान");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The shop that sells it. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Embedded
    @EmbeddedColumnNaming("name_%s")
    private LocalizedText name;

    @Embedded
    @EmbeddedColumnNaming("short_name_%s")
    private LocalizedText shortName;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @Embedded
    @EmbeddedColumnNaming("tag_%s")
    private @Nullable LocalizedText tag;

    @Enumerated(EnumType.STRING)
    private Tone tagTone = Tone.NEUTRAL;

    @Embedded
    @EmbeddedColumnNaming("pack_size_%s")
    private @Nullable LocalizedText packSize;

    @Embedded
    @EmbeddedColumnNaming("description_%s")
    private LocalizedText description;

    private int price;
    private @Nullable Integer mrp;
    /** A photo shipped with the app, e.g. "/images/products/..."; an uploaded photo replaces it. */
    private @Nullable String imageUrl;

    /** The shopkeeper's uploaded photo, served through a signed media URL. */
    @Embedded
    private @Nullable StoredPhoto photo;

    private @Nullable Double rating;

    @Embedded
    @EmbeddedColumnNaming("image_badge_%s")
    private @Nullable LocalizedText imageBadge;

    @Enumerated(EnumType.STRING)
    private Tone imageBadgeTone = Tone.NEUTRAL;

    /** Shown while the product is in stock, e.g. "Only 4 Left"; an out-of-stock product says so instead. */
    @Embedded
    @EmbeddedColumnNaming("stock_note_%s")
    private LocalizedText stockNote;

    @Enumerated(EnumType.STRING)
    private Tone stockTone = Tone.NEUTRAL;

    private String footerIcon;

    @Embedded
    @EmbeddedColumnNaming("footer_text_%s")
    private LocalizedText footerText;

    @Enumerated(EnumType.STRING)
    private Tone footerTone = Tone.NEUTRAL;

    private boolean flashDeal;
    private boolean inStock = true;

    @Column(unique = true, length = Ean13.LENGTH)
    private @Nullable String barcode;

    protected Product() {
    }

    public static Builder builder(Shop shop, LocalizedText name, LocalizedText shortName, ProductCategory category) {
        return new Builder(shop, name, shortName, category);
    }

    /** A product a shopkeeper lists from the shop portal; its stock note and footer are the standard ones. */
    static Product listedBy(Shop shop, ShopListing listing) {
        Product product = new Product();
        product.shop = shop;
        product.stockNote = LISTED_STOCK_NOTE;
        product.stockTone = Tone.SUCCESS;
        product.footerIcon = LISTED_FOOTER_ICON;
        product.footerText = LISTED_FOOTER;
        product.updateListing(listing);
        return product;
    }

    /** Applies what the shopkeeper entered; the short name follows the name. */
    void updateListing(ShopListing listing) {
        this.name = listing.name();
        this.shortName = listing.name();
        this.category = listing.category();
        this.packSize = listing.packSize();
        this.description = listing.description();
        this.price = listing.price();
        this.mrp = listing.mrp();
        this.inStock = listing.inStock();
        this.barcode = listing.barcode();
    }

    /** Shows the shopkeeper's photo instead of the one shipped with the app. */
    void replacePhoto(StoredPhoto photo) {
        this.photo = photo;
    }

    /** Sets a new selling price and MRP; the admin service checks them against {@link #MAX_PRICE} and each other. */
    void reprice(int price, @Nullable Integer mrp) {
        this.price = price;
        this.mrp = mrp;
    }

    /** Lists the product under "Depot Flash Deals", or takes it off. */
    void changeFlashDeal(boolean flashDeal) {
        this.flashDeal = flashDeal;
    }

    void changeStock(boolean inStock) {
        this.inStock = inStock;
    }

    public Long getId() {
        return id;
    }

    public Shop getShop() {
        return shop;
    }

    public @Nullable StoredPhoto getPhoto() {
        return photo;
    }

    public LocalizedText getName() {
        return name;
    }

    public LocalizedText getShortName() {
        return shortName;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public @Nullable LocalizedText getTag() {
        return tag;
    }

    public Tone getTagTone() {
        return tagTone;
    }

    public @Nullable LocalizedText getPackSize() {
        return packSize;
    }

    public LocalizedText getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public @Nullable Integer getMrp() {
        return mrp;
    }

    public @Nullable String getImageUrl() {
        return imageUrl;
    }

    public @Nullable Double getRating() {
        return rating;
    }

    public @Nullable LocalizedText getImageBadge() {
        return imageBadge;
    }

    public Tone getImageBadgeTone() {
        return imageBadgeTone;
    }

    public LocalizedText getStockNote() {
        return stockNote;
    }

    public Tone getStockTone() {
        return stockTone;
    }

    public String getFooterIcon() {
        return footerIcon;
    }

    public LocalizedText getFooterText() {
        return footerText;
    }

    public Tone getFooterTone() {
        return footerTone;
    }

    public boolean isFlashDeal() {
        return flashDeal;
    }

    public boolean isInStock() {
        return inStock;
    }

    public @Nullable String getBarcode() {
        return barcode;
    }

    /** Builds a catalogue item; description, price, stock note and footer are required. */
    public static final class Builder {

        private final Product product = new Product();

        private Builder(Shop shop, LocalizedText name, LocalizedText shortName, ProductCategory category) {
            product.shop = Objects.requireNonNull(shop, "shop");
            product.name = name;
            product.shortName = shortName;
            product.category = category;
        }

        public Builder tag(LocalizedText tag, Tone tone) {
            product.tag = tag;
            product.tagTone = tone;
            return this;
        }

        public Builder packSize(LocalizedText packSize) {
            product.packSize = packSize;
            return this;
        }

        public Builder description(LocalizedText description) {
            product.description = description;
            return this;
        }

        /** A price below the printed maximum retail price. */
        public Builder price(int price, int mrp) {
            product.price = price;
            product.mrp = mrp;
            return this;
        }

        /** A price with no MRP to compare against, e.g. a government-fixed seed price. */
        public Builder price(int price) {
            product.price = price;
            product.mrp = null;
            return this;
        }

        /** A photo; without one the frontend shows the category's icon tile. */
        public Builder image(String imageUrl) {
            product.imageUrl = imageUrl;
            return this;
        }

        public Builder rating(double rating) {
            product.rating = rating;
            return this;
        }

        public Builder imageBadge(LocalizedText badge, Tone tone) {
            product.imageBadge = badge;
            product.imageBadgeTone = tone;
            return this;
        }

        public Builder stockNote(LocalizedText note, Tone tone) {
            product.stockNote = note;
            product.stockTone = tone;
            return this;
        }

        public Builder footer(String icon, LocalizedText text, Tone tone) {
            product.footerIcon = icon;
            product.footerText = text;
            product.footerTone = tone;
            return this;
        }

        /** Lists the product under "Depot Flash Deals". */
        public Builder flashDeal() {
            product.flashDeal = true;
            return this;
        }

        public Builder outOfStock() {
            product.inStock = false;
            return this;
        }

        /**
         * @throws IllegalArgumentException when {@code ean13} is not a valid EAN-13 code
         */
        public Builder barcode(String ean13) {
            if (!Ean13.isValid(ean13)) {
                throw new IllegalArgumentException("Not a valid EAN-13 barcode: " + ean13);
            }
            product.barcode = ean13;
            return this;
        }

        public Product build() {
            Objects.requireNonNull(product.description, "description");
            Objects.requireNonNull(product.stockNote, "stockNote");
            Objects.requireNonNull(product.footerText, "footer");
            if (product.price <= 0 || product.price > MAX_PRICE) {
                throw new IllegalStateException("A product's price is 1 to %d rupees".formatted(MAX_PRICE));
            }
            if (product.mrp != null && product.mrp < product.price) {
                throw new IllegalStateException("A product's MRP is at least its price");
            }
            return product;
        }
    }
}
