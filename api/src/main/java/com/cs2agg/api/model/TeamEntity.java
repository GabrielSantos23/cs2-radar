package com.cs2agg.api.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class TeamEntity {
    private String id;
    private String name;
    private String acronym;
    private String imageUrl;

    public TeamEntity() {}

    public TeamEntity(String id, String name, String acronym, String imageUrl) {
        this.id = id;
        this.name = name;
        this.acronym = acronym;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
