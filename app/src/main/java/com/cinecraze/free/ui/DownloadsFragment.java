package com.cinecraze.free.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cinecraze.free.R;
import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.entities.DownloadItemEntity;

import java.util.ArrayList;
import java.util.List;

public class DownloadsFragment extends Fragment {

    private RecyclerView recyclerView;
    private DownloadsListAdapter adapter;
    private Handler handler;
    private final Runnable poller = new Runnable() {
        @Override public void run() { refreshStatuses(); handler.postDelayed(this, 1000); }
    };

    public static DownloadsFragment newInstance() { return new DownloadsFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.downloads_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DownloadsListAdapter(getContext());
        recyclerView.setAdapter(adapter);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override public void onResume() { super.onResume(); loadItems(); handler.post(poller); }
    @Override public void onPause() { super.onPause(); handler.removeCallbacks(poller); }

    private void loadItems() {
        List<DownloadItemEntity> items = CineCrazeDatabase.getInstance(getContext()).downloadItemDao().getAll();
        adapter.setItems(items);
    }

    private void refreshStatuses() {
        Context ctx = getContext();
        if (ctx == null) return;
        List<DownloadItemEntity> items = CineCrazeDatabase.getInstance(ctx).downloadItemDao().getAll();
        DownloadManager dm = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        for (DownloadItemEntity item : items) {
            DownloadManager.Query q = new DownloadManager.Query().setFilterById(item.downloadManagerId);
            try (Cursor c = dm.query(q)) {
                if (c != null && c.moveToFirst()) {
                    int bytesIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int localUriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    item.downloadedBytes = bytesIdx >= 0 ? c.getLong(bytesIdx) : 0L;
                    item.totalBytes = totalIdx >= 0 ? c.getLong(totalIdx) : 0L;
                    item.status = statusIdx >= 0 ? c.getInt(statusIdx) : item.status;
                    if (localUriIdx >= 0) item.localUri = c.getString(localUriIdx);
                    item.updatedAt = System.currentTimeMillis();
                    CineCrazeDatabase.getInstance(ctx).downloadItemDao().update(item);
                }
            } catch (Exception ignored) {}
        }
        adapter.setItems(items);
    }

    private static class DownloadsListAdapter extends RecyclerView.Adapter<DownloadsListAdapter.Holder> {
        private final Context context;
        private List<DownloadItemEntity> items = new ArrayList<>();
        DownloadsListAdapter(Context ctx) { this.context = ctx; }
        void setItems(List<DownloadItemEntity> items) { this.items = items != null ? items : new ArrayList<>(); notifyDataSetChanged(); }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.item_download, parent, false); return new Holder(v);
        }
        @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
            DownloadItemEntity it = items.get(pos);
            h.title.setText(it.title != null ? it.title : it.url);
            long total = it.totalBytes > 0 ? it.totalBytes : 1;
            int progress = (int) Math.min(100, (it.downloadedBytes * 100) / total);
            h.progressBar.setProgress(progress);
            h.status.setText(statusText(it.status));
        }
        @Override public int getItemCount() { return items.size(); }
        static class Holder extends RecyclerView.ViewHolder {
            TextView title; TextView status; ProgressBar progressBar;
            Holder(View v) { super(v); title = v.findViewById(R.id.download_title); status = v.findViewById(R.id.download_status); progressBar = v.findViewById(R.id.download_progress); }
        }
        private String statusText(int s) {
            switch (s) {
                case DownloadManager.STATUS_PENDING: return "Pending";
                case DownloadManager.STATUS_RUNNING: return "Downloading";
                case DownloadManager.STATUS_PAUSED: return "Paused";
                case DownloadManager.STATUS_SUCCESSFUL: return "Completed";
                case DownloadManager.STATUS_FAILED: return "Failed";
                default: return "Unknown";
            }
        }
    }
}