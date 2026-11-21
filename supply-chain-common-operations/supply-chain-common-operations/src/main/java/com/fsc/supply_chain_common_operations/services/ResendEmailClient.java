package com.fsc.supply_chain_common_operations.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ResendEmailClient {

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final RestClient restClient =
            RestClient.create("https://api.resend.com");

    public void send(
            List<String> to,
            List<String> cc,
            List<String> bcc,
            String subject,
            String html
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("from", "ProcureAxis <" + fromEmail + ">");
        request.put("to", to);
        if (cc != null && !cc.isEmpty()) request.put("cc", cc);
        if (bcc != null && !bcc.isEmpty()) request.put("bcc", bcc);
        request.put("subject", subject);
        request.put("html", html);

        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
