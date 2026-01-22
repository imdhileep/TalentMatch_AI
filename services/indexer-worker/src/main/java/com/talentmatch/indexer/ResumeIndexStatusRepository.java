package com.talentmatch.indexer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeIndexStatusRepository extends JpaRepository<ResumeIndexStatus, UUID> {
}
