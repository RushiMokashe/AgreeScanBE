package com.myagree.app.common.media;

import java.util.Optional;

/**
 * Serves one kind of uploaded file (scan photos, listing photos) through signed media URLs, because an
 * {@code <img>} tag cannot send a bearer token. A feature implements it as a bean and puts
 * {@code mediaUrlSigner.sign(kind(), id)} in its responses; {@code GET /api/media/{kind}/{id}} then checks the
 * signature and streams what {@link #load} returns.
 */
public interface MediaSource {

    /**
     * The URL segment naming this kind, e.g. "scan" in {@code /api/media/scan/42}: a lower-case letter followed by
     * up to 31 lower-case letters, digits or hyphens. Unique across sources.
     */
    String kind();

    /**
     * The file with this id, or empty when there is none. Runs without a signed-in user (the URL signature is the
     * authorization) and outside any transaction. Return photos only, never SVG or HTML.
     */
    Optional<MediaContent> load(String id);
}
