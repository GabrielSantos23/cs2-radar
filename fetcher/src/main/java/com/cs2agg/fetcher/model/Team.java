package com.cs2agg.fetcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Team(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("acronym") String acronym,
    @JsonProperty("image_url") String imageUrl
) {}
