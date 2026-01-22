package com.talentmatch.indexer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_index_status")
public class ResumeIndexStatus {
    @Id
    private UUID resumeId;

    private String status;

    @Column(columnDefinition = "TEXT")
    private String error;

    private OffsetDateTime indexedAt;

    public UUID getResumeId() {
        return resumeId;
    }

    public void setResumeId(UUID resumeId) {
        this.resumeId = resumeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public OffsetDateTime getIndexedAt() {
        return indexedAt;
    }

    public void setIndexedAt(OffsetDateTime indexedAt) {
        this.indexedAt = indexedAt;
    }
}
