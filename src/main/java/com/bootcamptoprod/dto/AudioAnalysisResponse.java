package com.bootcamptoprod.dto;

/**
 * Represents the final text response from the AI model, sent back to the client.
 * This DTO is used for all successful API responses.
 */
public record AudioAnalysisResponse(
        String response
) {
}