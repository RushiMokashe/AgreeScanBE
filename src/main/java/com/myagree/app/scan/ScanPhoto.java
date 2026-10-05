package com.myagree.app.scan;

import org.springframework.core.io.Resource;

/** An uploaded scan photo ready to stream back to the browser. */
public record ScanPhoto(Resource content, String contentType) {
}
