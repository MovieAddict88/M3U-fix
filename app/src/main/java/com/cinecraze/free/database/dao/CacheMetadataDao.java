package com.cinecraze.free.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cinecraze.free.database.entities.CacheMetadataEntity;

@Dao
public interface CacheMetadataDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CacheMetadataEntity metadata);
    
    @Query("SELECT * FROM cache_metadata WHERE key = :key")
    CacheMetadataEntity getMetadata(String key);
    
    @Query("DELETE FROM cache_metadata WHERE key = :key")
    void deleteMetadata(String key);
    
    @Query("DELETE FROM cache_metadata")
    void deleteAllMetadata();
}