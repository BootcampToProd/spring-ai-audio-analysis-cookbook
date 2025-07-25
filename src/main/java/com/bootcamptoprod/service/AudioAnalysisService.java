package com.bootcamptoprod.service;

import com.bootcamptoprod.dto.AudioAnalysisResponse;
import com.bootcamptoprod.dto.Base64Audio;
import com.bootcamptoprod.exception.AudioProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains the core business logic for the application. This service handles
 * converting all audio input types into a common format (Spring AI Media objects)
 * and uses the ChatClient to communicate with the multimodal AI model.
 */
@Service
public class AudioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AudioAnalysisService.class);

    // A single, reusable system prompt that defines the AI's persona and rules for audio.
    private static final String SYSTEM_PROMPT_TEMPLATE = getSystemPrompt();

    // A constant to programmatically check if the AI followed our rules.
    private static final String AI_ERROR_RESPONSE = "Error: I can only analyze audio and answer related questions.";

    private final ChatClient chatClient;

    // The ChatClient.Builder is injected by Spring, allowing us to build the client.
    public AudioAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // --- IMPLEMENTATION FOR SCENARIO 1: CLASSPATH ---
    public AudioAnalysisResponse analyzeAudioFromClasspath(String fileName, String prompt) {
        validatePrompt(prompt);

        if (!StringUtils.hasText(fileName)) {
            throw new AudioProcessingException("File name cannot be empty.");
        }

        // Assumes audio files are located in `src/main/resources/audio/`
        Resource audioResource = new ClassPathResource("audio/" + fileName);
        if (!audioResource.exists()) {
            throw new AudioProcessingException("File not found in classpath: audio/" + fileName);
        }

        // We assume MP3 for this example, but you could determine this dynamically.
        Media audioMedia = new Media(MimeType.valueOf("audio/mp3"), audioResource);

        return performAnalysis(prompt, List.of(audioMedia));
    }

    // --- IMPLEMENTATION FOR SCENARIO 2: MULTIPART FILES ---
    public AudioAnalysisResponse analyzeAudioFromFile(List<MultipartFile> audios, String prompt) {
        validatePrompt(prompt);

        if (audios == null || audios.isEmpty() || audios.stream().allMatch(MultipartFile::isEmpty)) {
            throw new AudioProcessingException("Audio files list cannot be empty.");
        }

        List<Media> mediaList = audios.stream()
                .filter(file -> !file.isEmpty())
                .map(this::convertMultipartFileToMedia) // Convert each file to a Media object
                .collect(Collectors.toList());

        return performAnalysis(prompt, mediaList);
    }

    // --- IMPLEMENTATION FOR SCENARIO 3: AUDIO URLS ---
    public AudioAnalysisResponse analyzeAudioFromUrl(List<String> audioUrls, String prompt) {
        validatePrompt(prompt);

        if (audioUrls == null || audioUrls.isEmpty()) {
            throw new AudioProcessingException("Audio URL list cannot be empty.");
        }

        List<Media> mediaList = audioUrls.stream()
                .map(this::convertUrlToMedia)
                .collect(Collectors.toList());

        return performAnalysis(prompt, mediaList);
    }

    // --- IMPLEMENTATION FOR SCENARIO 4: BASE64 ---
    public AudioAnalysisResponse analyzeAudioFromBase64(List<Base64Audio> base64Audios, String prompt) {
        validatePrompt(prompt);

        if (base64Audios == null || base64Audios.isEmpty()) {
            throw new AudioProcessingException("Base64 audio list cannot be empty.");
        }

        List<Media> mediaList = base64Audios.stream()
                .map(this::convertBase64ToMedia)
                .collect(Collectors.toList());

        return performAnalysis(prompt, mediaList);
    }


    // ===================================================================
    //               COMMON/REUSABLE PRIVATE METHODS
    // ===================================================================

    /**
     * This is the CORE method that communicates with the AI.
     * It is called by all the public service methods.
     */
    private AudioAnalysisResponse performAnalysis(String prompt, List<Media> mediaList) {
        if (mediaList.isEmpty()) {
            throw new AudioProcessingException("No valid audio files were provided for analysis.");
        }

        // This is where the magic happens: combining text and media in one call.
        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT_TEMPLATE)
                .user(userSpec -> userSpec
                        .text(prompt)                           // The user's text instruction
                        .media(mediaList.toArray(new Media[0]))) // The list of audio files
                .call()
                .content();

        // Check if the AI responded with our predefined error message (a "guardrail").
        if (AI_ERROR_RESPONSE.equalsIgnoreCase(response)) {
            throw new AudioProcessingException("The provided prompt is not related to audio analysis.");
        }

        return new AudioAnalysisResponse(response);
    }

    /**
     * Helper method for converting an uploaded MultipartFile into a Spring AI Media object.
     */
    private Media convertMultipartFileToMedia(MultipartFile file) {
        String contentType = file.getContentType();
        MimeType mimeType = determineAudioMimeType(contentType);
        return new Media(mimeType, file.getResource());
    }

    /**
     * Helper method for processing an audio file from a URL and converting it into a Media object.
     */
    private Media convertUrlToMedia(String audioUrl) {
        try {
            log.info("Processing audio from URL: {}", audioUrl);
            URL url = new URL(audioUrl);

            // Set timeouts to prevent the application from hanging on slow network requests.
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000); // 5-second timeout
            connection.setReadTimeout(5000);    // 5-second timeout

            // Get the MIME type from the URL's response headers to validate it's an audio file.
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                throw new AudioProcessingException("Invalid or non-audio MIME type for URL: " + audioUrl);
            }

            Resource audioResource = new UrlResource(audioUrl);
            return new Media(MimeType.valueOf(contentType), audioResource);
        } catch (Exception e) {
            throw new AudioProcessingException("Failed to download or process audio from URL: " + audioUrl, e);
        }
    }

    /**
     * Helper method for decoding a Base64 string into a Media object.
     */
    private Media convertBase64ToMedia(Base64Audio base64Audio) {
        if (!StringUtils.hasText(base64Audio.mimeType()) || !StringUtils.hasText(base64Audio.data())) {
            throw new AudioProcessingException("Base64 audio data and MIME type cannot be empty.");
        }
        try {
            // Decode the Base64 string back into its original binary format (byte array).
            byte[] decodedBytes = Base64.getDecoder().decode(base64Audio.data());
            // Wrap the byte array in a resource and create the Media object.
            return new Media(MimeType.valueOf(base64Audio.mimeType()), new ByteArrayResource(decodedBytes));
        } catch (Exception e) {
            throw new AudioProcessingException("Invalid Base64 data provided.", e);
        }
    }

    /**
     * Basic validation for the user's prompt.
     */
    private void validatePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new AudioProcessingException("Prompt cannot be empty.");
        }
    }

    /**
     * System prompt that defines the AI's behavior and boundaries for audio tasks.
     */
    private static String getSystemPrompt() {
        return """
                You are an AI assistant that specializes in audio analysis.
                Your task is to analyze the provided audio file(s) and answer the user's question.
                Common tasks are transcribing speech to text or summarizing the content.
                If the user's prompt is not related to analyzing the audio,
                respond with the exact phrase: 'Error: I can only analyze audio and answer related questions.'
                """;
    }

    /**
     * Helper method to determine MimeType from a content type string for common audio formats.
     */
    private MimeType determineAudioMimeType(String contentType) {
        if (contentType == null) {
            return MimeType.valueOf("audio/mp3"); // Default fallback
        }

        return switch (contentType.toLowerCase()) {
            case "audio/wav", "audio/x-wav" -> MimeType.valueOf("audio/wav");
            default -> MimeType.valueOf("audio/mp3");
        };
    }
}