package com.talentmatch.indexer;

import java.util.UUID;

public record ResumeResponse(UUID id, String filename, String contentType, String text) {
}
