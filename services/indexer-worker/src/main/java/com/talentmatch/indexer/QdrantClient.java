package com.talentmatch.indexer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class QdrantClient {
    private static final Logger logger = LoggerFactory.getLogger(QdrantClient.class);
    private static final int VECTOR_SIZE = 384;

    private final WebClient webClient;

    public QdrantClient(WebClient.Builder builder,
                        @Value("${qdrant.url:http://qdrant:6333}") String qdrantUrl) {
        this.webClient = builder.baseUrl(qdrantUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10))))
                .build();
    }

    @Bean
    public ApplicationRunner ensureCollection() {
        return args -> webClient.get()
                .uri("/collections/resumes")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(ex -> createCollection())
                .block(Duration.ofSeconds(20));
    }

    private Mono<String> createCollection() {
        logger.info("Creating Qdrant collection resumes");
        Map<String, Object> payload = Map.of(
                "vectors", Map.of("size", VECTOR_SIZE, "distance", "Cosine")
        );
        return webClient.put()
                .uri("/collections/resumes")
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(String.class);
    }

    public void upsertResumeVector(UUID resumeId, List<Double> vector) {
        Map<String, Object> point = Map.of(
                "id", resumeId.toString(),
                "vector", vector,
                "payload", Map.of()
        );
        Map<String, Object> body = Map.of("points", List.of(point));
        webClient.put()
                .uri("/collections/resumes/points?wait=true")
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(20));
    }
}
