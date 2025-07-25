package com.bootcamptoprod.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Custom exception that will result in an HTTP 400 Bad Request response
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AudioProcessingException extends RuntimeException {
    public AudioProcessingException(String message) {
        super(message);
    }

    public AudioProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}