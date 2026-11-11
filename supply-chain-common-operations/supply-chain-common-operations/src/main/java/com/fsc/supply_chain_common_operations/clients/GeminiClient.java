package com.fsc.supply_chain_common_operations.clients;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class GeminiClient {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";

    public String getAIResponse(String prompt) {

        // Get Gemini API key from environment variable
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is missing"
            );
        }

        // Configure HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        // Prepare Gemini request body
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        // Call Gemini API
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        GEMINI_ENDPOINT,
                        request,
                        Map.class
                );

        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null) {
            throw new RuntimeException(
                    "Empty response received from Gemini API"
            );
        }

        // Extract candidates
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) responseBody.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException(
                    "No response received from Gemini API"
            );
        }

        // Extract content
        Map<String, Object> candidate = candidates.get(0);

        Map<String, Object> content =
                (Map<String, Object>) candidate.get("content");

        if (content == null) {
            throw new RuntimeException(
                    "Gemini response content is empty"
            );
        }

        // Extract text
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");

        if (parts == null || parts.isEmpty()) {
            throw new RuntimeException(
                    "Gemini response parts are empty"
            );
        }

        Object text = parts.get(0).get("text");

        if (text == null) {
            throw new RuntimeException(
                    "Gemini response text is empty"
            );
        }

        return text.toString();
    }
}
