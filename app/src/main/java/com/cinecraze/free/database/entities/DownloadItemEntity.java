package com.cinecraze.free.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "download_items")
public class DownloadItemEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long downloadManagerId; // ID returned by DownloadManager

    public String title;
    public String url;

    public String localUri; // content:// URI if available

    public long totalBytes;
    public long downloadedBytes;

    public int status; // DownloadManager.STATUS_*

    public long createdAt;
    public long updatedAt;
}