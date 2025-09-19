package com.cinecraze.free.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.cinecraze.free.database.entities.DownloadItemEntity;

import java.util.List;

@Dao
public interface DownloadItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(DownloadItemEntity item);

    @Update
    int update(DownloadItemEntity item);

    @Query("SELECT * FROM download_items ORDER BY createdAt DESC")
    List<DownloadItemEntity> getAll();

    @Query("SELECT * FROM download_items WHERE downloadManagerId = :dmId LIMIT 1")
    DownloadItemEntity findByDownloadManagerId(long dmId);
}