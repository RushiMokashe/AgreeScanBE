package com.myagree.app.common.media;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * A stored file ready to stream to the browser.
 *
 * @param body        the bytes, e.g. a {@code FileSystemResource} or a {@code ByteArrayResource}
 * @param contentType the file's media type, e.g. {@code image/jpeg}
 */
public record MediaContent(Resource body, MediaType contentType) {
}
