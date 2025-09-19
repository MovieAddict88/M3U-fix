package com.cinecraze.free;

import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Season;
import com.cinecraze.free.R;
import com.google.gson.Gson;
import android.util.Log;

import java.util.List;

public class PaginatedMovieAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_PAGINATION = 1;
    
    private Context context;
    private List<Entry> entryList;
    private boolean isGridView;
    private PaginationListener paginationListener;
    
    // Pagination state
    private int currentPage = 0;
    private boolean hasMorePages = false;
    private int totalCount = 0;
    private boolean isLoading = false;

    public interface PaginationListener {
        void onPreviousPage();
        void onNextPage();
    }

    public PaginatedMovieAdapter(Context context, List<Entry> entryList, boolean isGridView) {
        this.context = context;
        this.entryList = entryList;
        this.isGridView = isGridView;
    }
    
    public void setPaginationListener(PaginationListener listener) {
        this.paginationListener = listener;
    }

    public void setGridView(boolean gridView) {
        isGridView = gridView;
        notifyDataSetChanged();
    }

    public void setEntryList(List<Entry> entryList) {
        this.entryList = entryList;
        notifyDataSetChanged();
    }
    
    public void updatePaginationState(int currentPage, boolean hasMorePages, int totalCount) {
        this.currentPage = currentPage;
        this.hasMorePages = hasMorePages;
        this.totalCount = totalCount;
        this.isLoading = false;
        notifyDataSetChanged();
    }
    
    public void setLoading(boolean loading) {
        this.isLoading = loading;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == entryList.size()) {
            return VIEW_TYPE_PAGINATION;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PAGINATION) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_pagination, parent, false);
            return new PaginationViewHolder(view);
        } else {
            View view;
            if (isGridView) {
                view = LayoutInflater.from(context).inflate(R.layout.item_grid, parent, false);
            } else {
                view = LayoutInflater.from(context).inflate(R.layout.item_list, parent, false);
            }
            return new MovieViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MovieViewHolder) {
            MovieViewHolder movieHolder = (MovieViewHolder) holder;
            Entry entry = entryList.get(position);

            movieHolder.title.setText(entry.getTitle());
            Glide.with(context).load(entry.getPoster()).into(movieHolder.poster);
            movieHolder.rating.setRating(entry.getRating());

            if (movieHolder.description != null) {
                movieHolder.description.setText(entry.getDescription());
            }
            if (movieHolder.year != null) {
                movieHolder.year.setText(String.valueOf(entry.getYear()));
            }
            if (movieHolder.country != null) {
                movieHolder.country.setText(entry.getCountry());
            }
            if (movieHolder.duration != null) {
                movieHolder.duration.setText(entry.getDuration());
            }

            // Set category badge (on poster) - Genre badge
            if (movieHolder.categoryBadge != null) {
                setCategoryBadge(movieHolder.categoryBadge, entry.getSubCategory());
            }
            
            // Set type badge (below title) - Content type badge
            if (movieHolder.typeBadge != null) {
                setTypeBadge(movieHolder.typeBadge, entry.getMainCategory());
            }

            movieHolder.itemView.setOnClickListener(v -> {
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
                    Log.w("PaginatedMovieAdapter", "Large series detected with " + totalEpisodes + " episodes. Using lightweight data transfer.");
                    
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
            });
        } else if (holder instanceof PaginationViewHolder) {
            PaginationViewHolder paginationHolder = (PaginationViewHolder) holder;
            
            // Handle Previous button
            paginationHolder.previousButton.setEnabled(currentPage > 0 && !isLoading);
            paginationHolder.previousButton.setOnClickListener(v -> {
                if (paginationListener != null && currentPage > 0 && !isLoading) {
                    paginationListener.onPreviousPage();
                }
            });
            
            // Handle Next button with additional validation
            boolean canGoNext = hasMorePages && !isLoading && ((currentPage + 1) * 20 < totalCount);
            paginationHolder.nextButton.setEnabled(canGoNext);
            paginationHolder.nextButton.setOnClickListener(v -> {
                boolean canClickNext = hasMorePages && !isLoading && ((currentPage + 1) * 20 < totalCount);
                if (paginationListener != null && canClickNext) {
                    paginationListener.onNextPage();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return entryList.size() + 1; // +1 for pagination footer
    }

    public class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView poster;
        TextView title;
        RatingBar rating;
        TextView description;
        TextView year;
        TextView country;
        TextView duration;
        TextView categoryBadge; // Added for category badge
        TextView typeBadge; // Added for type badge

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            poster = itemView.findViewById(R.id.poster);
            title = itemView.findViewById(R.id.title);
            rating = itemView.findViewById(R.id.rating);
            description = itemView.findViewById(R.id.description);
            year = itemView.findViewById(R.id.year);
            country = itemView.findViewById(R.id.country);
            duration = itemView.findViewById(R.id.duration);
            categoryBadge = itemView.findViewById(R.id.category_badge); // Initialize categoryBadge
            typeBadge = itemView.findViewById(R.id.type_badge); // Initialize typeBadge
        }
    }
    
    public class PaginationViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.button.MaterialButton previousButton;
        com.google.android.material.button.MaterialButton nextButton;

        public PaginationViewHolder(@NonNull View itemView) {
            super(itemView);
            previousButton = itemView.findViewById(R.id.btn_previous);
            nextButton = itemView.findViewById(R.id.btn_next);
        }
    }

    // Helper method to set category badge (genre badge)
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

    // Helper method to set type badge (content type badge)
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
}