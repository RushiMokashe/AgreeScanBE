package com.myagree.app.support;

import java.util.Optional;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import com.myagree.app.common.media.MediaContent;
import com.myagree.app.common.media.MediaSource;

/** Serves one sample photo as media kind {@value #KIND}, so signed media URLs can be tested without a feature. */
@TestComponent
public class TestMediaSource implements MediaSource {

    public static final String KIND = "test";
    public static final String SAMPLE_ID = "sample";
    public static final byte[] SAMPLE_PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

    @Override
    public String kind() {
        return KIND;
    }

    @Override
    public Optional<MediaContent> load(String id) {
        return SAMPLE_ID.equals(id)
                ? Optional.of(new MediaContent(new ByteArrayResource(SAMPLE_PNG), MediaType.IMAGE_PNG))
                : Optional.empty();
    }
}
