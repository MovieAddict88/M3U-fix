package com.cinecraze.free.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "entries")
public class EntryEntity {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "sub_category")
    private String subCategory;
    
    @ColumnInfo(name = "country")
    private String country;
    
    @ColumnInfo(name = "description")
    private String description;
    
    @ColumnInfo(name = "poster")
    private String poster;
    
    @ColumnInfo(name = "thumbnail")
    private String thumbnail;
    
    @ColumnInfo(name = "rating")
    private String rating; // Store as string to handle mixed types
    
    @ColumnInfo(name = "duration")
    private String duration;
    
    @ColumnInfo(name = "year")
    private String year; // Store as string to handle mixed types
    
    @ColumnInfo(name = "main_category")
    private String mainCategory; // To associate with category
    
    @ColumnInfo(name = "servers_json")
    private String serversJson; // Store servers as JSON string
    
    @ColumnInfo(name = "seasons_json")
    private String seasonsJson; // Store seasons as JSON string
    
    @ColumnInfo(name = "related_json")
    private String relatedJson; // Store related entries as JSON string
    
    // Constructor
    public EntryEntity() {}
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPoster() { return poster; }
    public void setPoster(String poster) { this.poster = poster; }
    
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    
    public String getMainCategory() { return mainCategory; }
    public void setMainCategory(String mainCategory) { this.mainCategory = mainCategory; }
    
    public String getServersJson() { return serversJson; }
    public void setServersJson(String serversJson) { this.serversJson = serversJson; }
    
    public String getSeasonsJson() { return seasonsJson; }
    public void setSeasonsJson(String seasonsJson) { this.seasonsJson = seasonsJson; }
    
    public String getRelatedJson() { return relatedJson; }
    public void setRelatedJson(String relatedJson) { this.relatedJson = relatedJson; }
}