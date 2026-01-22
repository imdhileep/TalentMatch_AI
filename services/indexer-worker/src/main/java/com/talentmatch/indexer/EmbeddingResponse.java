package com.talentmatch.indexer;

import java.util.List;

public record EmbeddingResponse(String id, List<Double> vector) {
}
