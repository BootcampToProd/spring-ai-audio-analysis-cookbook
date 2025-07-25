package com.bootcamptoprod.dto;

import java.util.List;

/**
 * Defines the API request body for analyzing one or more Base64 encoded audio files.
 */
public record Base64AudioAnalysisRequest(
        List<Base64Audio> base64AudioList,
        String prompt
) {
}