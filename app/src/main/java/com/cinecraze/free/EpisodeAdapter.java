package com.cinecraze.free;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cinecraze.free.models.Episode;
import com.cinecraze.free.R;

import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private Context context;
    private List<Episode> episodes;
    private OnEpisodeClickListener listener;
    private int selectedEpisodePosition = 0;

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode, int position);
        void onEpisodeDownload(Episode episode, int position);
    }

    public EpisodeAdapter(Context context, List<Episode> episodes, OnEpisodeClickListener listener) {
        this.context = context;
        this.episodes = episodes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_episode, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Episode episode = episodes.get(position);
        
        // Set episode title
        holder.episodeTitle.setText(episode.getTitle());
        
        // Set episode description if available
        if (episode.getDescription() != null && !episode.getDescription().isEmpty()) {
            holder.episodeDescription.setText(episode.getDescription());
            holder.episodeDescription.setVisibility(View.VISIBLE);
        } else {
            holder.episodeDescription.setVisibility(View.GONE);
        }
        
        // Set episode duration
        if (episode.getDuration() != null && !episode.getDuration().isEmpty()) {
            holder.episodeDuration.setText(episode.getDuration());
            holder.episodeDuration.setVisibility(View.VISIBLE);
        } else {
            holder.episodeDuration.setVisibility(View.GONE);
        }
        
        // Load episode thumbnail with optimization
        if (episode.getThumbnail() != null && !episode.getThumbnail().isEmpty()) {
            Glide.with(context)
                .load(episode.getThumbnail())
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .skipMemoryCache(false) // Use memory cache for better performance
                .into(holder.episodeThumbnail);
        } else {
            holder.episodeThumbnail.setImageResource(R.drawable.image_placeholder);
        }
        
        // Highlight selected episode
        holder.itemView.setSelected(position == selectedEpisodePosition);
        
        // Show/hide viewed indicator
        if (position < selectedEpisodePosition) {
            holder.viewedIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.viewedIndicator.setVisibility(View.GONE);
        }
        
        // Set click listeners for direct episode play (CinemaX style)
        View.OnClickListener episodeClickListener = v -> {
            int oldPosition = selectedEpisodePosition;
            selectedEpisodePosition = position;
            // Only update the affected items instead of entire dataset for better performance
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedEpisodePosition);
            if (listener != null) {
                listener.onEpisodeClick(episode, position);
            }
        };
        
        // Both the entire item and the play button should trigger episode play
        holder.itemView.setOnClickListener(episodeClickListener);
        holder.playButton.setOnClickListener(episodeClickListener);
        
        // Download button functionality
        holder.downloadButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEpisodeDownload(episode, position);
            }
        });

        // Show episode download only if at least one direct file exists
        boolean hasDirect = false;
        if (episode.getServers() != null) {
            for (com.cinecraze.free.models.Server s : episode.getServers()) {
                if (com.cinecraze.free.utils.VideoServerUtils.isDirectFileUrl(s.getUrl())) { hasDirect = true; break; }
            }
        }
        holder.downloadButton.setVisibility(hasDirect ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return episodes != null ? episodes.size() : 0;
    }

    public void setSelectedEpisode(int position) {
        int oldPosition = selectedEpisodePosition;
        selectedEpisodePosition = position;
        // Only update the affected items for better performance
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedEpisodePosition);
    }

    public void updateEpisodes(List<Episode> newEpisodes) {
        // Simple update for now - can be optimized with DiffUtil later if needed
        this.episodes = newEpisodes;
        this.selectedEpisodePosition = 0; // Reset selection
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView episodeThumbnail;
        ImageView playButton;
        ImageView downloadButton;
        ImageView viewedIndicator;
        TextView episodeTitle;
        TextView episodeDescription;
        TextView episodeDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Map to CinemaX-style IDs
            episodeThumbnail = itemView.findViewById(R.id.image_view_item_episode_thumbail);
            playButton = itemView.findViewById(R.id.image_view_item_episode_play);
            downloadButton = itemView.findViewById(R.id.image_view_item_episode_download);
            viewedIndicator = itemView.findViewById(R.id.image_view_item_episode_viewed);
            episodeTitle = itemView.findViewById(R.id.text_view_item_episode_title);
            episodeDescription = itemView.findViewById(R.id.text_view_item_episode_description);
            episodeDuration = itemView.findViewById(R.id.text_view_item_episode_duration);
        }
    }
}