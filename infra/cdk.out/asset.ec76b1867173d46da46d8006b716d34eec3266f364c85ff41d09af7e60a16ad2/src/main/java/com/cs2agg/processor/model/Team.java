package com.cs2agg.processor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class Team {
    private String id;
    private String name;
    private String acronym;
    private String imageUrl;

    public Team() {}

    public Team(String id, String name, String acronym, String imageUrl) {
        this.id = id;
        this.name = name;
        this.acronym = acronym;
        this.imageUrl = imageUrl;
    }

    @JsonProperty("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @JsonProperty("name")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @JsonProperty("acronym")
    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }

    @JsonProperty("image_url")
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
