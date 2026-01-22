package com.talentmatch.match;

import java.util.UUID;

public record JobMatchResponse(UUID id, String title, String company, String location, String description, double score) {
}
