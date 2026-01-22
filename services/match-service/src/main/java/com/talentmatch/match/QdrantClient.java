package com.talentmatch.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class QdrantClient {
    private static final Logger logger = LoggerFactory.getLogger(QdrantClient.class);
    private static final int VECTOR_SIZE = 384;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${qdrant.collection:jobs}")
    private String collectionName;

    public QdrantClient(WebClient.Builder builder,
                        ObjectMapper objectMapper,
                        @Value("${qdrant.url:http://qdrant:6333}") String qdrantUrl) {
        this.webClient = builder.baseUrl(qdrantUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(10))))
                .build();
        this.objectMapper = objectMapper;
    }

    @Bean
    public ApplicationRunner ensureCollection() {
        return args -> {
            webClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.NOT_FOUND,
                            response -> Mono.error(new IllegalStateException("missing")))
                    .bodyToMono(String.class)
                    .onErrorResume(ex -> createCollection())
                    .block(Duration.ofSeconds(20));
        };
    }

    private Mono<String> createCollection() {
        logger.info("Creating Qdrant collection {}", collectionName);
        Map<String, Object> payload = Map.of(
                "vectors", Map.of("size", VECTOR_SIZE, "distance", "Cosine")
        );
        return webClient.put()
                .uri("/collections/{name}", collectionName)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(String.class);
    }

    public void upsertJobVector(UUID jobId, List<Double> vector) {
        Map<String, Object> point = Map.of(
                "id", jobId.toString(),
                "vector", vector,
                "payload", Map.of()
        );
        Map<String, Object> body = Map.of("points", List.of(point));
        webClient.put()
                .uri("/collections/{name}/points?wait=true", collectionName)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(20));
    }

    public List<JobMatch> searchJobs(List<Double> resumeVector, int topK, JobRepository jobRepository) {
        Map<String, Object> body = Map.of(
                "vector", resumeVector,
                "limit", topK,
                "with_payload", true
        );
        String response = webClient.post()
                .uri("/collections/{name}/points/search", collectionName)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(20));

        List<JobMatch> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            for (JsonNode item : root.path("result")) {
                String id = item.path("id").asText();
                double score = item.path("score").asDouble();
                jobRepository.findById(UUID.fromString(id))
                        .ifPresent(job -> results.add(new JobMatch(job, score)));
            }
        } catch (Exception ex) {
            logger.error("Failed to parse Qdrant search response", ex);
        }
        return results;
    }

    public List<Double> fetchResumeVector(UUID resumeId) {
        String response = webClient.get()
                .uri("/collections/resumes/points/{id}?with_vector=true", resumeId.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(20));
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode vectorNode = root.path("result").path("vector");
            List<Double> vector = new ArrayList<>();
            vectorNode.forEach(node -> vector.add(node.asDouble()));
            return vector;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse resume vector", ex);
        }
    }
}
