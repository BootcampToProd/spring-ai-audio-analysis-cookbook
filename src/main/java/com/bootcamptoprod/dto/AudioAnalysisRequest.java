package com.bootcamptoprod.dto;

import java.util.List;

/**
 * Defines the API request body for analyzing audio from URLs or a single classpath file.
 */
public record AudioAnalysisRequest(
        List<String> audioUrls,
        String prompt,
        String fileName
) {
}
