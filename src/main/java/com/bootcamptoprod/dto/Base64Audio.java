package com.bootcamptoprod.dto;

/**
 * Represents a single audio file encoded as a Base64 string, including its MIME type.
 */
public record Base64Audio(
        String mimeType,
        String data  // The Base64 encoded audio string
) {
}
