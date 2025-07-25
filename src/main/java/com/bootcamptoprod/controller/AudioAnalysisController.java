package com.bootcamptoprod.controller;

import com.bootcamptoprod.dto.AudioAnalysisRequest;
import com.bootcamptoprod.dto.AudioAnalysisResponse;
import com.bootcamptoprod.dto.Base64AudioAnalysisRequest;
import com.bootcamptoprod.exception.AudioProcessingException;
import com.bootcamptoprod.service.AudioAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Exposes the REST API endpoints for all four audio analysis scenarios.
 * This controller acts as the entry point for all incoming web requests
 * and delegates the core logic to the AudioAnalysisService.
 */
@RestController
@RequestMapping("/api/v1/audio/analysis")
public class AudioAnalysisController {

    private final AudioAnalysisService audioAnalysisService;

    // Constructor Injection
    public AudioAnalysisController(AudioAnalysisService audioAnalysisService) {
        this.audioAnalysisService = audioAnalysisService;
    }

    /**
     * SCENARIO 1: Analyze a single audio file from the classpath (e.g., src/main/resources/audio).
     */
    @PostMapping("/from-classpath")
    public ResponseEntity<AudioAnalysisResponse> analyzeFromClasspath(@RequestBody AudioAnalysisRequest request) {
        AudioAnalysisResponse response = audioAnalysisService.analyzeAudioFromClasspath(request.fileName(), request.prompt());
        return ResponseEntity.ok(response);
    }

    /**
     * SCENARIO 2: Analyze one or more audio files uploaded by the user.
     * This endpoint handles multipart/form-data requests.
     */
    @PostMapping(value = "/from-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioAnalysisResponse> analyzeFromFiles(@RequestParam("audioFiles") List<MultipartFile> audioFiles, @RequestParam("prompt") String prompt) {
        AudioAnalysisResponse response = audioAnalysisService.analyzeAudioFromFile(audioFiles, prompt);
        return ResponseEntity.ok(response);
    }

    /**
     * SCENARIO 3: Analyze one or more audio files from a list of URLs.
     */
    @PostMapping("/from-urls")
    public ResponseEntity<AudioAnalysisResponse> analyzeFromUrls(@RequestBody AudioAnalysisRequest request) {
        AudioAnalysisResponse response = audioAnalysisService.analyzeAudioFromUrl(request.audioUrls(), request.prompt());
        return ResponseEntity.ok(response);
    }

    /**
     * SCENARIO 4: Analyze one or more audio files from Base64-encoded strings.
     */
    @PostMapping("/from-base64")
    public ResponseEntity<AudioAnalysisResponse> analyzeFromBase64(@RequestBody Base64AudioAnalysisRequest request) {
        AudioAnalysisResponse response = audioAnalysisService.analyzeAudioFromBase64(request.base64AudioList(), request.prompt());
        return ResponseEntity.ok(response);
    }

    /**
     * Centralized exception handler for this controller.
     * Catches our custom exception from the service layer and returns a clean
     * HTTP 400 Bad Request with the error message.
     */
    @ExceptionHandler(AudioProcessingException.class)
    public ResponseEntity<AudioAnalysisResponse> handleAudioProcessingException(AudioProcessingException ex) {
        return ResponseEntity.badRequest().body(new AudioAnalysisResponse(ex.getMessage()));
    }
}
