package com.myagree.app.dashboard.dto;

/**
 * Mirrors {@code Helpline} in frontend/src/lib/types.ts.
 *
 * @param phone         dialable number for tel: links
 * @param displayNumber formatted for reading, e.g. "1800-180-1551"
 */
public record HelplineResponse(String name, String phone, String displayNumber, String hours) {
}
