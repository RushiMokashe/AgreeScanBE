package com.myagree.app.common.media;

import java.time.Duration;

/**
 * A file opened through a signed media URL.
 *
 * @param content           the file
 * @param remainingValidity how much longer the URL is valid, and so how long the browser may cache the file
 */
record SignedMedia(MediaContent content, Duration remainingValidity) {
}
