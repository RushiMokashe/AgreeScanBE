package com.myagree.app.mandi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

import org.jspecify.annotations.Nullable;

/** A bulk purchase order from an institutional buyer that farmers can respond to directly. */
@Entity
public class BuyerInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_id")
    private MandiMarket market;

    private String buyerName;
    private boolean verified;
    private String subtitle;
    private String icon;
    private String badge;
    private boolean badgeHighlighted;
    private int volumeTonnes;
    private String produce;
    private String priceLabel;
    private int pricePerQuintal;
    private String priceNote;
    private boolean priceNoteHighlighted;
    private boolean highlighted;
    private @Nullable String specsTitle;

    @ElementCollection
    @CollectionTable(name = "buyer_inquiry_spec", joinColumns = @JoinColumn(name = "inquiry_id"))
    @OrderColumn(name = "position")
    @Column(name = "spec")
    private List<String> specs = new ArrayList<>();

    private String footerIcon;
    private String footerText;
    private String actionLabel;
    private @Nullable String actionIcon;
    private boolean actionPrimary;
    private boolean responded;

    protected BuyerInquiry() {
    }

    public static Builder builder(MandiMarket market, String buyerName, String subtitle) {
        return new Builder(market, buyerName, subtitle);
    }

    /** Records the farmer's response; responding again changes nothing. */
    public void respond() {
        responded = true;
    }

    public Long getId() {
        return id;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getIcon() {
        return icon;
    }

    public String getBadge() {
        return badge;
    }

    public boolean isBadgeHighlighted() {
        return badgeHighlighted;
    }

    public int getVolumeTonnes() {
        return volumeTonnes;
    }

    public String getProduce() {
        return produce;
    }

    public String getPriceLabel() {
        return priceLabel;
    }

    public int getPricePerQuintal() {
        return pricePerQuintal;
    }

    public String getPriceNote() {
        return priceNote;
    }

    public boolean isPriceNoteHighlighted() {
        return priceNoteHighlighted;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public @Nullable String getSpecsTitle() {
        return specsTitle;
    }

    public List<String> getSpecs() {
        return List.copyOf(specs);
    }

    public String getFooterIcon() {
        return footerIcon;
    }

    public String getFooterText() {
        return footerText;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public @Nullable String getActionIcon() {
        return actionIcon;
    }

    public boolean isActionPrimary() {
        return actionPrimary;
    }

    public boolean isResponded() {
        return responded;
    }

    /** Builds an inquiry card; icon, badge, requirement, price, footer and action are required. */
    public static final class Builder {

        private final BuyerInquiry inquiry = new BuyerInquiry();

        private Builder(MandiMarket market, String buyerName, String subtitle) {
            inquiry.market = market;
            inquiry.buyerName = buyerName;
            inquiry.subtitle = subtitle;
        }

        public Builder verified() {
            inquiry.verified = true;
            return this;
        }

        public Builder icon(String icon) {
            inquiry.icon = icon;
            return this;
        }

        public Builder badge(String badge, boolean highlighted) {
            inquiry.badge = badge;
            inquiry.badgeHighlighted = highlighted;
            return this;
        }

        public Builder requirement(int volumeTonnes, String produce) {
            inquiry.volumeTonnes = volumeTonnes;
            inquiry.produce = produce;
            return this;
        }

        public Builder price(String label, int pricePerQuintal, String note, boolean noteHighlighted) {
            inquiry.priceLabel = label;
            inquiry.pricePerQuintal = pricePerQuintal;
            inquiry.priceNote = note;
            inquiry.priceNoteHighlighted = noteHighlighted;
            return this;
        }

        /** Shows produce and price in the primary colour. */
        public Builder highlighted() {
            inquiry.highlighted = true;
            return this;
        }

        public Builder specs(String title, List<String> specs) {
            inquiry.specsTitle = title;
            inquiry.specs = new ArrayList<>(specs);
            return this;
        }

        public Builder footer(String icon, String text) {
            inquiry.footerIcon = icon;
            inquiry.footerText = text;
            return this;
        }

        public Builder action(String label, @Nullable String icon, boolean primary) {
            inquiry.actionLabel = label;
            inquiry.actionIcon = icon;
            inquiry.actionPrimary = primary;
            return this;
        }

        public BuyerInquiry build() {
            Objects.requireNonNull(inquiry.icon, "icon");
            Objects.requireNonNull(inquiry.badge, "badge");
            Objects.requireNonNull(inquiry.produce, "requirement");
            Objects.requireNonNull(inquiry.priceLabel, "price");
            Objects.requireNonNull(inquiry.footerText, "footer");
            Objects.requireNonNull(inquiry.actionLabel, "action");
            return inquiry;
        }
    }
}
