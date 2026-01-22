package com.talentmatch.resume;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resumes")
public class ResumeController {
    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);
    private final ResumeRepository resumeRepository;
    private final KafkaTemplate<String, ResumeCreatedEvent> kafkaTemplate;

    public ResumeController(ResumeRepository resumeRepository,
                            KafkaTemplate<String, ResumeCreatedEvent> kafkaTemplate) {
        this.resumeRepository = resumeRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String extractedText;
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(document);
        }

        Resume resume = new Resume();
        resume.setFilename(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setExtractedText(extractedText);
        resume.setCreatedAt(OffsetDateTime.now());
        Resume saved = resumeRepository.save(resume);

        kafkaTemplate.send("resumes.created", saved.getId().toString(), new ResumeCreatedEvent(saved.getId()))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to publish resume.created for {}", saved.getId(), ex);
                    } else {
                        logger.info("Published resume.created for {}", saved.getId());
                    }
                });

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResumeResponse(saved.getId(), saved.getFilename(), saved.getContentType(), saved.getExtractedText()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> get(@PathVariable UUID id) {
        return resumeRepository.findById(id)
                .map(resume -> ResponseEntity.ok(new ResumeResponse(resume.getId(), resume.getFilename(),
                        resume.getContentType(), resume.getExtractedText())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
