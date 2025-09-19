package com.cinecraze.free.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.annotation.NonNull;

@Entity(tableName = "cache_metadata")
public class CacheMetadataEntity {
    
    @PrimaryKey
    @NonNull
    private String key;
    
    @ColumnInfo(name = "last_updated")
    private long lastUpdated;
    
    @ColumnInfo(name = "data_version")
    private String dataVersion;
    
    // Constructor
    public CacheMetadataEntity() {}
    
    @Ignore
    public CacheMetadataEntity(@NonNull String key, long lastUpdated, String dataVersion) {
        this.key = key;
        this.lastUpdated = lastUpdated;
        this.dataVersion = dataVersion;
    }
    
    // Getters and setters
    public String getKey() { return key; }
    public void setKey(@NonNull String key) { this.key = key; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getDataVersion() { return dataVersion; }
    public void setDataVersion(String dataVersion) { this.dataVersion = dataVersion; }
}