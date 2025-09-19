package com.cinecraze.free.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.dao.DownloadItemDao;
import com.cinecraze.free.database.entities.DownloadItemEntity;

public class DownloadManagerHelper {

    public static long enqueueDownload(Context context, String title, String url) {
        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(url);

            String safeTitle = sanitizeFileName(title != null ? title : "download");
            String fileName = safeTitle + guessExtension(url);

            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setTitle(title)
                    .setDescription("Downloading...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName);

            long downloadId = dm.enqueue(request);

            DownloadItemEntity entity = new DownloadItemEntity();
            entity.downloadManagerId = downloadId;
            entity.title = title;
            entity.url = url;
            entity.status = DownloadManager.STATUS_PENDING;
            entity.createdAt = System.currentTimeMillis();
            entity.updatedAt = entity.createdAt;

            CineCrazeDatabase db = CineCrazeDatabase.getInstance(context);
            DownloadItemDao dao = db.downloadItemDao();
            dao.insert(entity);

            return downloadId;
        } catch (Exception e) {
            return -1L;
        }
    }

    public static void refreshStatus(Context context, DownloadItemEntity entity) {
        if (entity == null) return;
        try {
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query q = new DownloadManager.Query();
            q.setFilterById(entity.downloadManagerId);
            try (Cursor c = dm.query(q)) {
                if (c != null && c.moveToFirst()) {
                    int bytesIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int localUriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                    entity.downloadedBytes = bytesIdx >= 0 ? c.getLong(bytesIdx) : 0L;
                    entity.totalBytes = totalIdx >= 0 ? c.getLong(totalIdx) : 0L;
                    entity.status = statusIdx >= 0 ? c.getInt(statusIdx) : entity.status;
                    if (localUriIdx >= 0) {
                        entity.localUri = c.getString(localUriIdx);
                    }
                    entity.updatedAt = System.currentTimeMillis();

                    CineCrazeDatabase.getInstance(context).downloadItemDao().update(entity);
                }
            }
        } catch (Exception ignored) { }
    }

    private static String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String guessExtension(String url) {
        String lower = url != null ? url.toLowerCase() : "";
        if (lower.contains(".mp4")) return ".mp4";
        if (lower.contains(".mkv")) return ".mkv";
        if (lower.contains(".webm")) return ".webm";
        if (lower.contains(".avi")) return ".avi";
        return ".mp4";
    }
}