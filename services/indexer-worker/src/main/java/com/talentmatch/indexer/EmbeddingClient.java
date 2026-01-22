package com.talentmatch.indexer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class EmbeddingClient {
    private final WebClient webClient;

    public EmbeddingClient(WebClient.Builder builder,
                           @Value("${embedding.url:http://embedding-service:8000}") String embeddingUrl) {
        this.webClient = builder.baseUrl(embeddingUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        reactor.netty.http.client.HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(30))))
                .build();
    }

    public List<Double> embed(String id, String text) {
        Map<String, Object> payload = Map.of("id", id, "text", text);
        EmbeddingResponse response = webClient.post()
                .uri("/embed")
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block(Duration.ofSeconds(30));
        return response.vector();
    }
}
