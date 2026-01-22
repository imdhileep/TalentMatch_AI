package com.talentmatch.match;

import java.util.List;

public record EmbeddingResponse(String id, List<Double> vector) {
}
