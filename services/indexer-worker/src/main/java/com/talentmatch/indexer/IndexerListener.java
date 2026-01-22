package com.talentmatch.indexer;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IndexerListener {
    private static final Logger logger = LoggerFactory.getLogger(IndexerListener.class);
    private final ResumeClient resumeClient;
    private final EmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;
    private final ResumeIndexStatusRepository statusRepository;

    public IndexerListener(ResumeClient resumeClient,
                           EmbeddingClient embeddingClient,
                           QdrantClient qdrantClient,
                           ResumeIndexStatusRepository statusRepository) {
        this.resumeClient = resumeClient;
        this.embeddingClient = embeddingClient;
        this.qdrantClient = qdrantClient;
        this.statusRepository = statusRepository;
    }

    @KafkaListener(topics = "resumes.created", groupId = "indexer-worker")
    public void handleResumeCreated(ResumeCreatedEvent event) {
        UUID resumeId = event.resumeId();
        ResumeIndexStatus status = statusRepository.findById(resumeId)
                .orElseGet(() -> {
                    ResumeIndexStatus created = new ResumeIndexStatus();
                    created.setResumeId(resumeId);
                    return created;
                });
        try {
            ResumeResponse resume = resumeClient.fetchResume(resumeId);
            var vector = embeddingClient.embed(resumeId.toString(), resume.text());
            qdrantClient.upsertResumeVector(resumeId, vector);

            status.setStatus("INDEXED");
            status.setError(null);
            status.setIndexedAt(OffsetDateTime.now());
            statusRepository.save(status);

            logger.info("Indexed resume {}", resumeId);
        } catch (Exception ex) {
            status.setStatus("FAILED");
            status.setError(ex.getMessage());
            status.setIndexedAt(OffsetDateTime.now());
            statusRepository.save(status);
            logger.error("Failed to index resume {}", resumeId, ex);
        }
    }
}
