package com.myagree.app.common.spi;

/**
 * One slice's part of what the assistant knows about a farmer; see {@link FarmContextContributor}.
 *
 * @param title   a short heading in the requested language, e.g. "Plots"
 * @param content plain-text facts in the requested language: a few short lines, without markup
 */
public record FarmContextSection(String title, String content) {
}
