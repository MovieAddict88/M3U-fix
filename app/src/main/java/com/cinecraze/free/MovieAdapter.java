package com.cinecraze.free;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.bumptech.glide.Glide;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Season;
import com.cinecraze.free.R;
import com.google.gson.Gson;

import java.util.List;
import android.util.Log;
import android.widget.Toast;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {

    private Context context;
    private List<Entry> entryList;
    private boolean isGridView;

    public MovieAdapter(Context context, List<Entry> entryList, boolean isGridView) {
        this.context = context;
        this.entryList = entryList;
        this.isGridView = isGridView;
    }

    public void setGridView(boolean gridView) {
        isGridView = gridView;
    }

    public void setEntryList(List<Entry> entryList) {
        this.entryList = entryList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isGridView) {
            view = LayoutInflater.from(context).inflate(R.layout.item_grid, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_list, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entry entry = entryList.get(position);

        holder.title.setText(entry.getTitle());
        Glide.with(context).load(entry.getPoster()).into(holder.poster);
        holder.rating.setRating(entry.getRating());

        if (holder.description != null) {
            holder.description.setText(entry.getDescription());
        }
        if (holder.year != null) {
            holder.year.setText(String.valueOf(entry.getYear()));
        }
        if (holder.country != null) {
            holder.country.setText(entry.getCountry());
        }
        if (holder.duration != null) {
            holder.duration.setText(entry.getDuration());
        }
        
        // Set category badge (on poster) - Genre badge
        if (holder.categoryBadge != null) {
            setCategoryBadge(holder.categoryBadge, entry.getSubCategory());
        }
        
        // Set type badge (below title) - Content type badge
        if (holder.typeBadge != null) {
            setTypeBadge(holder.typeBadge, entry.getMainCategory());
        }

        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(context, DetailsActivity.class);
                
                // Check if entry has too many episodes (potential TransactionTooLargeException)
                int totalEpisodes = 0;
                if (entry.getSeasons() != null) {
                    for (Season season : entry.getSeasons()) {
                        if (season.getEpisodes() != null) {
                            totalEpisodes += season.getEpisodes().size();
                        }
                    }
                }
                
                // If too many episodes, pass only essential data to avoid TransactionTooLargeException
                if (totalEpisodes > 500) { // Conservative limit
                    Log.w("MovieAdapter", "Large series detected with " + totalEpisodes + " episodes. Using lightweight data transfer.");
                    
                    // Create a lightweight entry with only essential data
                    Entry lightweightEntry = new Entry();
                    lightweightEntry.setTitle(entry.getTitle());
                    lightweightEntry.setSubCategory(entry.getSubCategory());
                    lightweightEntry.setMainCategory(entry.getMainCategory());
                    lightweightEntry.setCountry(entry.getCountry());
                    lightweightEntry.setDescription(entry.getDescription());
                    lightweightEntry.setPoster(entry.getPoster());
                    lightweightEntry.setThumbnail(entry.getThumbnail());
                    lightweightEntry.setRating(entry.getRating());
                    lightweightEntry.setDuration(entry.getDuration());
                    lightweightEntry.setYear(entry.getYear());
                    lightweightEntry.setServers(entry.getServers());
                    // Don't set seasons - will be loaded separately in DetailsActivity
                    
                    intent.putExtra("entry", new Gson().toJson(lightweightEntry));
                    intent.putExtra("needsFullData", true);
                    intent.putExtra("entryId", entry.getId());
                } else {
                    // For smaller series, pass the full entry
                    intent.putExtra("entry", new Gson().toJson(entry));
                }
                
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("MovieAdapter", "Error starting DetailsActivity: " + e.getMessage(), e);
                Toast.makeText(context, "Error opening content details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    private void setCategoryBadge(TextView badge, String category) {
        String badgeText;
        int badgeColor;
        
        if (category == null || category.trim().isEmpty()) {
            category = "Other";
        }
        
        // For genre badge, use the category as-is and apply genre-specific colors
        badgeText = category.toUpperCase();
        if (badgeText.length() > 8) {
            badgeText = badgeText.substring(0, 8);
        }
        
        // Apply genre-specific colors
        if (category.toLowerCase().contains("action")) {
            badgeColor = ContextCompat.getColor(context, R.color.badge_live_tv); // Red for action
        } else if (category.toLowerCase().contains("drama")) {
            badgeColor = ContextCompat.getColor(context, R.color.badge_series); // Green for drama
        } else if (category.toLowerCase().contains("comedy")) {
            badgeColor = ContextCompat.getColor(context, R.color.badge_movies); // Light blue for comedy
        } else if (category.toLowerCase().contains("horror")) {
            badgeColor = ContextCompat.getColor(context, R.color.badge_live_tv); // Red for horror
        } else if (category.toLowerCase().contains("romance")) {
            badgeColor = ContextCompat.getColor(context, R.color.badge_series); // Green for romance
        } else {
            badgeColor = ContextCompat.getColor(context, R.color.badge_default); // Default orange
        }
        
        badge.setText(badgeText);
        
        // Create colored background
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(badgeColor);
        background.setCornerRadius(4 * context.getResources().getDisplayMetrics().density);
        badge.setBackground(background);
    }
    
    private void setTypeBadge(TextView badge, String category) {
        String badgeText;
        int badgeColor;
        
        if (category == null || category.trim().isEmpty()) {
            category = "Other";
        }
        
        // Determine badge text and color based on content type from mainCategory
        String lowerCategory = category.toLowerCase();
        
        if (lowerCategory.contains("live")) {
            badgeText = "LIVE";
            badgeColor = ContextCompat.getColor(context, R.color.type_badge_live); // Red
        } else if (lowerCategory.contains("movie") || lowerCategory.contains("film")) {
            badgeText = "MOVIE";
            badgeColor = ContextCompat.getColor(context, R.color.type_badge_movies); // Light blue
        } else if (lowerCategory.contains("series") || lowerCategory.contains("tv")) {
            badgeText = "SERIES";
            badgeColor = ContextCompat.getColor(context, R.color.type_badge_series); // Green
        } else {
            badgeText = category.toUpperCase();
            if (badgeText.length() > 6) {
                badgeText = badgeText.substring(0, 6);
            }
            badgeColor = ContextCompat.getColor(context, R.color.type_badge_default); // Orange
        }
        
        badge.setText(badgeText);
        
        // Create colored background
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(badgeColor);
        background.setCornerRadius(3 * context.getResources().getDisplayMetrics().density);
        badge.setBackground(background);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView title;
        RatingBar rating;
        TextView description;
        TextView year;
        TextView country;
        TextView duration;
        TextView categoryBadge;
        TextView typeBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.poster);
            title = itemView.findViewById(R.id.title);
            rating = itemView.findViewById(R.id.rating);
            description = itemView.findViewById(R.id.description);
            year = itemView.findViewById(R.id.year);
            country = itemView.findViewById(R.id.country);
            duration = itemView.findViewById(R.id.duration);
            categoryBadge = itemView.findViewById(R.id.category_badge);
            typeBadge = itemView.findViewById(R.id.type_badge);
        }
    }
}
