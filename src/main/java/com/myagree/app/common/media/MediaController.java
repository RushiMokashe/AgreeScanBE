package com.myagree.app.common.media;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Streams uploaded files to {@code <img>} tags; public, because the URL signature is the authorization. */
@RestController
@RequestMapping("/api/media")
class MediaController {

    private static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    /** Uploads are user content: nothing in them may run, even when the URL is opened directly. */
    private static final String SANDBOXED = "sandbox";

    private final MediaService mediaService;

    MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/{kind}/{id}")
    ResponseEntity<Resource> media(@PathVariable String kind, @PathVariable String id,
                                   @RequestParam long exp, @RequestParam String sig) {
        SignedMedia media = mediaService.open(kind, id, exp, sig);
        return ResponseEntity.ok()
                .contentType(media.content().contentType())
                .cacheControl(CacheControl.maxAge(media.remainingValidity()).cachePrivate())
                .header(CONTENT_SECURITY_POLICY, SANDBOXED)
                .body(media.content().body());
    }
}
