package com.myagree.app.support;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

/** Reads values out of MockMvc JSON responses. */
public final class JsonBodies {

    private JsonBodies() {
    }

    public static <T> T read(MvcResult result, String jsonPath) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), jsonPath);
    }

    public static long readId(MvcResult result, String jsonPath) throws UnsupportedEncodingException {
        Number id = read(result, jsonPath);
        return id.longValue();
    }
}
