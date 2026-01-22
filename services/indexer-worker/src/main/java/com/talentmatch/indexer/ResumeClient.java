package com.talentmatch.indexer;

import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ResumeClient {
    private final WebClient webClient;

    public ResumeClient(WebClient.Builder builder,
                        @Value("${resume.url:http://resume-service:8081}") String resumeUrl) {
        this.webClient = builder.baseUrl(resumeUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(15))))
                .build();
    }

    public ResumeResponse fetchResume(UUID resumeId) {
        return webClient.get()
                .uri("/resumes/{id}", resumeId)
                .retrieve()
                .bodyToMono(ResumeResponse.class)
                .block(Duration.ofSeconds(15));
    }
}
