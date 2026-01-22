package com.talentmatch.match;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class MatchController {
    private static final Logger logger = LoggerFactory.getLogger(MatchController.class);
    private final JobRepository jobRepository;
    private final EmbeddingClient embeddingClient;
    private final QdrantClient qdrantClient;

    public MatchController(JobRepository jobRepository,
                           EmbeddingClient embeddingClient,
                           QdrantClient qdrantClient) {
        this.jobRepository = jobRepository;
        this.embeddingClient = embeddingClient;
        this.qdrantClient = qdrantClient;
    }

    @PostMapping("/jobs")
    public ResponseEntity<Job> createJob(@RequestBody JobRequest request) {
        Job job = new Job();
        job.setTitle(request.title());
        job.setCompany(request.company());
        job.setLocation(request.location());
        job.setDescription(request.description());
        Job saved = jobRepository.save(job);

        String combined = String.join(" ", request.title(), request.company(), request.location(), request.description());
        List<Double> vector = embeddingClient.embed(saved.getId().toString(), combined);
        qdrantClient.upsertJobVector(saved.getId(), vector);
        logger.info("Indexed job {} into Qdrant", saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/jobs")
    public List<Job> listJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/match/resume/{resumeId}")
    public ResponseEntity<List<JobMatchResponse>> matchResume(@PathVariable UUID resumeId,
                                                              @RequestParam(defaultValue = "5") int topK) {
        try {
            List<Double> resumeVector = qdrantClient.fetchResumeVector(resumeId);
            List<JobMatch> matches = qdrantClient.searchJobs(resumeVector, topK, jobRepository);
            List<JobMatchResponse> response = matches.stream()
                    .map(match -> new JobMatchResponse(match.job().getId(), match.job().getTitle(),
                            match.job().getCompany(), match.job().getLocation(),
                            match.job().getDescription(), match.score()))
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Failed to match resume {}", resumeId, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
