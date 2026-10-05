package com.myagree.app.store;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * A store department tile: a title with a second line in Devanagari under it, e.g. "Crop Medicines" over
 * "फसल सुरक्षा व कीटनाशक" in English, and a fuller local name under the title in Marathi and Hindi.
 */
@Entity
public class StoreCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true)
    private ProductCategory category;

    @Embedded
    @EmbeddedColumnNaming("title_%s")
    private LocalizedText title;

    @Embedded
    @EmbeddedColumnNaming("local_title_%s")
    private LocalizedText localTitle;

    @Embedded
    @EmbeddedColumnNaming("description_%s")
    private LocalizedText description;

    private String icon;

    @Embedded
    @EmbeddedColumnNaming("badge_%s")
    private LocalizedText badge;

    protected StoreCategory() {
    }

    public StoreCategory(ProductCategory category, LocalizedText title, LocalizedText localTitle,
                         LocalizedText description, String icon, LocalizedText badge) {
        this.category = category;
        this.title = title;
        this.localTitle = localTitle;
        this.description = description;
        this.icon = icon;
        this.badge = badge;
    }

    public Long getId() {
        return id;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public LocalizedText getTitle() {
        return title;
    }

    public LocalizedText getLocalTitle() {
        return localTitle;
    }

    public LocalizedText getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public LocalizedText getBadge() {
        return badge;
    }
}
