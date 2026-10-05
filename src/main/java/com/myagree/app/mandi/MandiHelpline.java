package com.myagree.app.mandi;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.hibernate.annotations.EmbeddedColumnNaming;

import com.myagree.app.common.i18n.LocalizedText;

/**
 * @param phone         dialable number for tel: links, e.g. "18002331020"
 * @param displayNumber formatted for reading, e.g. "1800-233-1020"
 * @param hours         when the helpline answers, e.g. "6 AM - 8 PM"
 */
@Embeddable
public record MandiHelpline(
        @Column(name = "helpline_phone") String phone,
        @Column(name = "helpline_display") String displayNumber,
        @EmbeddedColumnNaming("helpline_hours_%s") LocalizedText hours) {
}
