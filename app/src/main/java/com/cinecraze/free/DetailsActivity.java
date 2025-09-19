package com.cinecraze.free;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cinecraze.free.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Season;
import com.cinecraze.free.models.Episode;
import com.cinecraze.free.models.Server;
import com.cinecraze.free.repository.DataRepository;
import com.cinecraze.free.utils.VideoServerUtils;
import com.cinecraze.free.player.CustomPlayerFragment;
import com.google.gson.Gson;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = "DetailsActivity";

    // UI Components
    private TextView title;
    private TextView description;
    private RecyclerView relatedContentRecyclerView;

    // CinemaX-style UI components
    private ImageView imageViewMovieBackground;
    private ImageView imageViewMovieCover;
    private TextView textViewMovieTitle;
    private TextView textViewMovieDescription;
    private TextView textViewMovieYear;
    private TextView textViewMovieDuration;
    private RatingBar ratingBarMovieRating;
    private RecyclerView recyclerViewMovieGenres;
    // Removed my list and share components
    private FloatingActionButton floatingActionButtonPlay;

    // Removed server selector components

    // Server dialog
    private Dialog serverSelectionDialog;
    private LinearLayoutManager linearLayoutManagerServers;

    // TV Series components
    private androidx.appcompat.widget.AppCompatSpinner seasonSpinner;
    private LinearLayout seriesSeasonsContainer;
    private RecyclerView episodeRecyclerView;

    // Enhanced video source selection
    private int currentServerIndex = 0;
    private int currentSeasonIndex = 0;
    private SmartServerSpinner smartServerSpinner;
    private boolean isInFullscreen = false;

    // CinemaX Player Fragment
    private CustomPlayerFragment customPlayerFragment;

    // Data
    private Entry currentEntry;
    private Season currentSeason;
    private Episode currentEpisode;
    private List<Server> currentServers;
    private EpisodeAdapter episodeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        try {
            initializeViews();
            setupData();
            // REMOVED: setupVideoPlayer(); - Don't auto-start video player
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading content: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        // Old components for compatibility
        title = findViewById(R.id.title);
        description = findViewById(R.id.description);
        relatedContentRecyclerView = findViewById(R.id.related_content_recycler_view);

        // Initialize CinemaX-style UI components
        imageViewMovieBackground = findViewById(R.id.image_view_activity_movie_background);
        imageViewMovieCover = findViewById(R.id.image_view_activity_movie_cover);
        textViewMovieTitle = findViewById(R.id.text_view_activity_movie_title);
        textViewMovieDescription = findViewById(R.id.text_view_activity_movie_description);
        textViewMovieYear = findViewById(R.id.text_view_activity_movie_year);
        textViewMovieDuration = findViewById(R.id.text_view_activity_movie_duration);
        ratingBarMovieRating = findViewById(R.id.rating_bar_activity_movie_rating);
        recyclerViewMovieGenres = findViewById(R.id.recycle_view_activity_movie_genres);
        floatingActionButtonPlay = findViewById(R.id.floating_action_button_activity_movie_play);

        // Initialize TV Series components (CinemaX-style)
        seriesSeasonsContainer = findViewById(R.id.linear_layout_activity_serie_seasons);
        seasonSpinner = findViewById(R.id.spinner_activity_serie_season_list);
        episodeRecyclerView = findViewById(R.id.recycle_view_activity_activity_serie_episodes);

        // Setup click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Floating action button click listener (main play button)
        floatingActionButtonPlay.setOnClickListener(v -> showServerSelectionDialog());
    }

    private void setupData() {
        currentEntry = getEntryFromIntent();
        if (currentEntry != null) {
            // Update legacy components for compatibility
            if (title != null) title.setText(currentEntry.getTitle());
            if (description != null) description.setText(currentEntry.getDescription());

            // Update CinemaX-style UI components
            if (textViewMovieTitle != null) {
                textViewMovieTitle.setText(currentEntry.getTitle());
            }
            if (textViewMovieDescription != null) {
                textViewMovieDescription.setText(currentEntry.getDescription());
            }

            // Set movie details if available
            if (textViewMovieYear != null && currentEntry.getYear() > 0) {
                textViewMovieYear.setText(String.valueOf(currentEntry.getYear()));
            }
            if (textViewMovieDuration != null && currentEntry.getDuration() != null) {
                textViewMovieDuration.setText(currentEntry.getDuration() + " min");
            }
            if (ratingBarMovieRating != null && currentEntry.getRating() > 0) {
                try {
                    float rating = currentEntry.getRating();
                    ratingBarMovieRating.setRating(rating / 2.0f); // Convert to 5-star scale
                } catch (Exception e) {
                    ratingBarMovieRating.setVisibility(View.GONE);
                }
            }

            // Load movie images
            loadMovieImages();

            // Setup server selector
            setupServerSelector();

            // Setup TV Series components ONLY if it's a TV series
            if ("TV Series".equalsIgnoreCase(currentEntry.getMainCategory()) ||
                "Series".equalsIgnoreCase(currentEntry.getMainCategory()) ||
                "TV".equalsIgnoreCase(currentEntry.getMainCategory())) {
                // For large series, seasons might be loaded asynchronously
                // So we'll setup TV components after data loading
                setupTVSeriesComponents();

                // Hide floating play button for TV series to avoid confusion
                // Users should select episodes from the seasons section
                if (floatingActionButtonPlay != null) {
                    floatingActionButtonPlay.setVisibility(View.GONE);
                    Log.d(TAG, "FLOATING PLAY BUTTON HIDDEN for TV series: " + currentEntry.getMainCategory());
                } else {
                    Log.d(TAG, "FloatingActionButton is null!");
                }
                Log.d(TAG, "TV Series detected - play button should be hidden: " + currentEntry.getMainCategory());
            } else {
                // Hide season selector for movies, live TV, and other content
                if (seriesSeasonsContainer != null) {
                    seriesSeasonsContainer.setVisibility(View.GONE);
                    Log.d(TAG, "SEASONS section hidden for: " + currentEntry.getMainCategory());
                }

                // Show floating play button for movies and live TV
                if (floatingActionButtonPlay != null) {
                    floatingActionButtonPlay.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Floating play button shown for: " + currentEntry.getMainCategory());
                }
                Log.d(TAG, "Non-TV series content detected: " + currentEntry.getMainCategory());
            }

            // Setup related content
            setupRelatedContent();
        } else {
            Log.e(TAG, "No entry data received");
            Toast.makeText(this, "No content data available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadMovieImages() {
        // Use Glide to load thumbnail and poster images
        if (imageViewMovieBackground != null && currentEntry.getThumbnail() != null) {
            Glide.with(this).load(currentEntry.getThumbnail()).into(imageViewMovieBackground);
        }
        if (imageViewMovieCover != null && currentEntry.getPoster() != null) {
            Glide.with(this).load(currentEntry.getPoster()).into(imageViewMovieCover);
        }
    }

    private void setupServerSelector() {
        currentServers = getCurrentServers();
        // Server selector UI components removed - now handled by floating play button dialog
    }

    private void setupTVSeriesComponents() {
        if (currentEntry.getSeasons() == null || currentEntry.getSeasons().isEmpty()) {
            seriesSeasonsContainer.setVisibility(View.GONE);
            Toast.makeText(this, "No seasons available for this TV series.", Toast.LENGTH_LONG).show();
            return;
        }
        seriesSeasonsContainer.setVisibility(View.VISIBLE);
        setupSeasonSpinner();
        if (!currentEntry.getSeasons().isEmpty()) {
            currentSeason = currentEntry.getSeasons().get(0);
            currentSeasonIndex = 0;
            // User feedback for very large episode loads
            if (currentSeason.getEpisodes() != null && currentSeason.getEpisodes().size() >= 1000) {
                Toast.makeText(this, "Warning: This season has " + currentSeason.getEpisodes().size() + " episodes. Loading may take a while.", Toast.LENGTH_LONG).show();
            }
            loadEpisodesAsync();
        }
    }

    /**
     * Load episodes asynchronously to prevent UI blocking for large series
     */
    private void loadEpisodesAsync() {
        // Show loading indicator
        showEpisodeLoadingIndicator(true);

        // Use background thread for episode setup
        new Thread(() -> {
            try {
                // Simulate any heavy processing that might be needed
                Thread.sleep(100); // Small delay to show loading state

                // Switch back to UI thread for UI updates
                runOnUiThread(() -> {
                    try {
                        showEpisodeLoadingIndicator(false);
                        setupEpisodeAdapter();
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting up episode adapter: " + e.getMessage(), e);
                        showEpisodeLoadingIndicator(false);
                        Toast.makeText(DetailsActivity.this, "Error loading episodes", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                runOnUiThread(() -> {
                    showEpisodeLoadingIndicator(false);
                    Toast.makeText(DetailsActivity.this, "Loading interrupted", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Show/hide episode loading indicator
     */
    private void showEpisodeLoadingIndicator(boolean show) {
        if (episodeRecyclerView != null) {
            episodeRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }

        // You can add a progress bar here if needed
        // For now, we'll just hide/show the RecyclerView
    }

    private void setupSeasonSpinner() {
        if (seasonSpinner != null && currentEntry.getSeasons() != null && !currentEntry.getSeasons().isEmpty()) {
            // Create season names array for spinner
            String[] seasonNames = new String[currentEntry.getSeasons().size()];
            for (int i = 0; i < currentEntry.getSeasons().size(); i++) {
                Season season = currentEntry.getSeasons().get(i);
                int episodeCount = season.getEpisodes() != null ? season.getEpisodes().size() : 0;
                seasonNames[i] = "Season " + season.getSeason() + " (" + episodeCount + " episodes)";
            }

            // Create and set adapter
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, seasonNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            seasonSpinner.setAdapter(adapter);

            // Set spinner selection listener with async episode loading
            seasonSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position != currentSeasonIndex && position < currentEntry.getSeasons().size()) {
                        handleSeasonChange(position);
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    // Do nothing
                }
            });
        }
    }

    /**
     * Handle season change with async loading for better performance
     */
    private void handleSeasonChange(int newSeasonIndex) {
        if (newSeasonIndex < 0 || newSeasonIndex >= currentEntry.getSeasons().size()) {
            return;
        }

        Season newSeason = currentEntry.getSeasons().get(newSeasonIndex);
        int episodeCount = newSeason.getEpisodes() != null ? newSeason.getEpisodes().size() : 0;

        Log.d(TAG, "Changing to season " + (newSeasonIndex + 1) + " with " + episodeCount + " episodes");

        // Show loading state for large season changes
        if (episodeCount > 100) {
            showEpisodeLoadingIndicator(true);
            Toast.makeText(this, "Loading " + episodeCount + " episodes...", Toast.LENGTH_SHORT).show();
        }

        // Update season data
        int oldSeasonIndex = currentSeasonIndex;
        currentSeasonIndex = newSeasonIndex;
        currentSeason = newSeason;
        currentEpisode = null; // Reset episode
        currentServerIndex = 0; // Reset server index

        // Load episodes asynchronously to prevent UI blocking
        new Thread(() -> {
            try {
                // Small delay for large seasons to show loading state
                if (episodeCount > 100) {
                    Thread.sleep(150);
                }

                runOnUiThread(() -> {
                    try {
                        setupEpisodeAdapter();
                        updateServerSelector();

                        if (episodeCount > 100) {
                            showEpisodeLoadingIndicator(false);
                        }

                        Log.d(TAG, "Season change completed: " + (oldSeasonIndex + 1) + " -> " + (newSeasonIndex + 1));
                    } catch (Exception e) {
                        Log.e(TAG, "Error during season change: " + e.getMessage(), e);
                        showEpisodeLoadingIndicator(false);
                        Toast.makeText(DetailsActivity.this, "Error loading season episodes", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                runOnUiThread(() -> {
                    showEpisodeLoadingIndicator(false);
                    Toast.makeText(DetailsActivity.this, "Season loading interrupted", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setupEpisodeAdapter() {
        if (currentSeason == null || currentSeason.getEpisodes() == null || currentSeason.getEpisodes().isEmpty()) {
            Toast.makeText(this, "No episodes available for this season.", Toast.LENGTH_LONG).show();
            episodeRecyclerView.setVisibility(View.GONE);
            return;
        }
        episodeRecyclerView.setVisibility(View.VISIBLE);
        // Performance optimizations for large episode lists
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "IndexOutOfBoundsException in RecyclerView: " + e.getMessage());
                }
            }
        };

        // Optimize for large datasets
        layoutManager.setInitialPrefetchItemCount(Math.min(currentSeason.getEpisodes().size(), 10));
        episodeRecyclerView.setLayoutManager(layoutManager);

        // Enhanced performance optimizations for very large episode lists
        //episodeRecyclerView.setHasFixedSize(true); // Items have fixed size
        episodeRecyclerView.setItemViewCacheSize(20); // Increased cache for large lists
        episodeRecyclerView.setDrawingCacheEnabled(true);
        episodeRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

        // Enable nested scrolling for better performance
        episodeRecyclerView.setNestedScrollingEnabled(true);

        // For very large episode lists (500+), implement virtualization
        if (currentSeason.getEpisodes().size() > 500) {
            Log.i(TAG, "Large episode list detected (" + currentSeason.getEpisodes().size() + " episodes). Enabling optimizations.");

            // Use a more conservative item view cache for memory efficiency
            episodeRecyclerView.setItemViewCacheSize(15);

            // Enable view recycling pool for better memory management
            RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
            viewPool.setMaxRecycledViews(0, 30); // Allow up to 30 recycled views
            episodeRecyclerView.setRecycledViewPool(viewPool);

            // Show a warning message for very large lists
            Toast.makeText(this,
                "Loading " + currentSeason.getEpisodes().size() + " episodes. Scroll may take a moment to initialize.",
                Toast.LENGTH_LONG).show();
        }

        // Create adapter with optimized episode click handling
        episodeAdapter = new EpisodeAdapter(this, currentSeason.getEpisodes(), new EpisodeAdapter.OnEpisodeClickListener() {
            @Override
            public void onEpisodeClick(Episode episode, int position) {
                // Handle episode selection efficiently
                handleEpisodeSelection(episode, position);
            }

            @Override
            public void onEpisodeDownload(Episode episode, int position) {
                // Handle episode download
                downloadEpisode(episode);
            }
        });

        // Enable stable IDs for better performance with large datasets
        episodeAdapter.setHasStableIds(false); // Set to true if you implement getItemId()
        episodeRecyclerView.setAdapter(episodeAdapter);

        // Select first episode by default but don't auto-scroll for large lists
        if (!currentSeason.getEpisodes().isEmpty()) {
            currentEpisode = currentSeason.getEpisodes().get(0);

            // Only scroll to position for smaller lists to avoid performance issues
            if (currentSeason.getEpisodes().size() <= 100) {
                episodeRecyclerView.scrollToPosition(0);
            }
        }
    }

    /**
     * Handle episode selection with optimized performance
     */
    private void handleEpisodeSelection(Episode episode, int position) {
        currentEpisode = episode;
        currentServerIndex = 0; // Reset server index for new episode

        // Update server list for the selected episode efficiently
        currentServers = getCurrentServers();

        // Show server selection dialog for episode
        showServerSelectionDialog();

        // Log selection for debugging large lists
        Log.d(TAG, "Selected episode " + (position + 1) + " of " + currentSeason.getEpisodes().size());
    }

    private void showServerSpinner() {
        // Server spinner UI removed - functionality moved to floating action button dialog
    }

    private void showSeasonSpinner() {
        if (currentEntry.getSeasons() != null && currentEntry.getSeasons().size() > 1) {
            String[] seasonNames = new String[currentEntry.getSeasons().size()];
            for (int i = 0; i < currentEntry.getSeasons().size(); i++) {
                seasonNames[i] = "Season " + currentEntry.getSeasons().get(i).getSeason();
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Select Season");
            builder.setSingleChoiceItems(seasonNames, currentSeasonIndex, (dialog, which) -> {
                currentSeasonIndex = which;
                currentSeason = currentEntry.getSeasons().get(which);
                currentEpisode = null; // Reset episode
                currentServerIndex = 0; // Reset server index
                setupEpisodeAdapter();
                updateServerSelector();
                dialog.dismiss();
            });
            builder.show();
        } else {
            Toast.makeText(this, "Only one season available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateServerButtonText() {
        // Server button removed - no longer needed
    }

    private void updateServerInfo() {
        // Server info text removed - no longer needed
    }



    private void updateServerSelector() {
        currentServers = getCurrentServers();
        // Server selector UI removed - functionality moved to floating play button
    }

    private void setupVideoPlayer() {
        String videoUrl = getCurrentVideoUrl();

        if (videoUrl != null && !videoUrl.isEmpty()) {
            try {
                // Use the enhanced CustomPlayerFragment that handles both embedded and direct videos
                customPlayerFragment = CustomPlayerFragment.newInstance(
                    videoUrl,
                    false, // isLive
                    VideoServerUtils.getVideoType(videoUrl),
                    currentEntry != null ? currentEntry.getTitle() : "Video",
                    currentEntry != null ? currentEntry.getDescription() : "",
                    currentEntry != null ? currentEntry.getImageUrl() : "",
                    currentEntry != null ? currentEntry.getId() : 0,
                    "movie" // or "tv" based on content type
                );

                // Add fragment to container
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.player_container, customPlayerFragment);
                transaction.commit();

            } catch (Exception e) {
                Log.e(TAG, "Error setting up video player: " + e.getMessage(), e);
                Toast.makeText(this, "Error setting up video player", Toast.LENGTH_LONG).show();
            }
        } else {
            Log.w(TAG, "No video URL available");
            // Removed toast message as requested
        }
    }

    private String getCurrentVideoUrl() {
        if (currentServers != null && currentServers.size() > currentServerIndex) {
            String url = currentServers.get(currentServerIndex).getUrl();
            return VideoServerUtils.enhanceVideoUrl(url);
        }
        return null;
    }

    private List<Server> getCurrentServers() {
        // For TV series, get servers from current episode
        if (currentEpisode != null && currentEpisode.getServers() != null && !currentEpisode.getServers().isEmpty()) {
            return sortServersByPreference(new ArrayList<>(currentEpisode.getServers()));
        }
        // For movies, get servers from current entry
        if (currentEntry != null) {
            List<Server> servers = currentEntry.getServers();
            return servers != null ? sortServersByPreference(new ArrayList<>(servers)) : null;
        }
        return null;
    }

    private List<Server> sortServersByPreference(List<Server> servers) {
        if (servers == null || servers.isEmpty()) return servers;
        Collections.sort(servers, new Comparator<Server>() {
            @Override
            public int compare(Server a, Server b) {
                int pa = getDomainPreference(a.getUrl());
                int pb = getDomainPreference(b.getUrl());
                if (pa != pb) return Integer.compare(pa, pb);
                String an = a.getName() != null ? a.getName() : "";
                String bn = b.getName() != null ? b.getName() : "";
                return an.compareToIgnoreCase(bn);
            }
        });
        return servers;
    }

    private int getDomainPreference(String url) {
        if (url == null) return 999;
        String u = url.toLowerCase();
        if (u.contains("vidlink.pro")) return 0;
        if (u.contains("embed.su")) return 1;
        if (u.contains("vidsrc.xyz")) return 2;
        if (u.contains("vidsrc.net")) return 3;
        if (u.contains("player.autoembed.cc") || u.contains("autoembed.cc")) return 4;
        return 100;
    }

    private Entry getEntryFromIntent() {
        try {
            String entryJson = getIntent().getStringExtra("entry");
            if (entryJson != null && !entryJson.isEmpty()) {
                Gson gson = new Gson();
                Entry entry = gson.fromJson(entryJson, Entry.class);

                // Check if we need to load full data (for large series)
                boolean needsFullData = getIntent().getBooleanExtra("needsFullData", false);
                int entryId = getIntent().getIntExtra("entryId", 0);

                if (needsFullData && entryId != 0) {
                    Log.i(TAG, "Loading full data for large series with ID: " + entryId);
                    // Load full data from database or repository
                    loadFullEntryData(entry, entryId);
                }

                return entry;
            }

            Log.e(TAG, "No entry data found in intent");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing entry from intent: " + e.getMessage(), e);
            return null;
        }
    }

    private void loadFullEntryData(Entry entry, int entryId) {
        // Load full entry data from database or repository asynchronously
        // This prevents TransactionTooLargeException for large series
        Log.i(TAG, "Starting async load for large series with ID: " + entryId);

        // Show loading indicator
        showEpisodeLoadingIndicator(true);
        Toast.makeText(this, "Loading TV series data...", Toast.LENGTH_SHORT).show();

        // Use background thread to prevent UI blocking
        new Thread(() -> {
            try {
                DataRepository dataRepository = new DataRepository(this);

                Log.i(TAG, "Loading full data for large series with ID: " + entryId);

                // Load the complete entry data from the repository
                List<Entry> allEntries = dataRepository.getAllCachedEntries();
                Entry foundEntry = null;

                // Find the entry with the matching ID
                for (Entry fullEntry : allEntries) {
                    if (fullEntry.getId() == entryId) {
                        foundEntry = fullEntry;
                        break;
                    }
                }

                final Entry finalEntry = foundEntry;

                // Switch back to UI thread for UI updates
                runOnUiThread(() -> {
                    try {
                        if (finalEntry != null && finalEntry.getSeasons() != null && !finalEntry.getSeasons().isEmpty()) {
                            // Copy the seasons data from the full entry
                            entry.setSeasons(finalEntry.getSeasons());
                            Log.i(TAG, "Loaded " + finalEntry.getSeasons().size() + " seasons for large series");

                            // Re-setup TV series components with the loaded data
                            setupTVSeriesComponents();

                            // Calculate total episodes for user feedback
                            int totalEpisodes = 0;
                            for (Season season : finalEntry.getSeasons()) {
                                if (season.getEpisodes() != null) {
                                    totalEpisodes += season.getEpisodes().size();
                                }
                            }

                            // Show success message
                            Toast.makeText(DetailsActivity.this,
                                "Loaded " + finalEntry.getSeasons().size() + " seasons with " + totalEpisodes + " episodes",
                                Toast.LENGTH_LONG).show();
                        } else {
                            // Fallback to manual creation if loading fails
                            Log.w(TAG, "Entry not found in cache, using fallback method");
                            loadFallbackSeasonData(entry, entryId);
                        }

                        showEpisodeLoadingIndicator(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing loaded entry data: " + e.getMessage(), e);
                        showEpisodeLoadingIndicator(false);
                        loadFallbackSeasonData(entry, entryId);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading cached entries: " + e.getMessage(), e);

                // Fallback to manual creation on UI thread
                runOnUiThread(() -> {
                    showEpisodeLoadingIndicator(false);
                    loadFallbackSeasonData(entry, entryId);
                });
            }
        }).start();
    }

    /**
     * Fallback method to create seasons data when database loading fails
     */
    private void loadFallbackSeasonData(Entry entry, int entryId) {
        // Show loading indicator for fallback
        showEpisodeLoadingIndicator(true);
        Toast.makeText(this, "Generating episode data...", Toast.LENGTH_SHORT).show();

        // Use background thread for fallback generation
        new Thread(() -> {
            try {
                List<Season> seasons = createSeasonsForLargeSeries(entryId);

                runOnUiThread(() -> {
                    try {
                        if (seasons != null && !seasons.isEmpty()) {
                            entry.setSeasons(seasons);
                            setupTVSeriesComponents();

                            // Calculate total episodes for user feedback
                            int totalEpisodes = 0;
                            for (Season season : seasons) {
                                if (season.getEpisodes() != null) {
                                    totalEpisodes += season.getEpisodes().size();
                                }
                            }

                            Toast.makeText(DetailsActivity.this,
                                "Generated " + seasons.size() + " seasons with " + totalEpisodes + " episodes",
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(DetailsActivity.this, "Unable to load episode data", Toast.LENGTH_LONG).show();
                        }

                        showEpisodeLoadingIndicator(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting up fallback seasons: " + e.getMessage(), e);
                        showEpisodeLoadingIndicator(false);
                        Toast.makeText(DetailsActivity.this, "Error loading episode data", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in fallback season creation: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showEpisodeLoadingIndicator(false);
                    Toast.makeText(DetailsActivity.this, "Failed to generate episode data", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private List<Season> createSeasonsForLargeSeries(int entryId) {
        // This is an optimized solution to create seasons data for large series
        // In production, this should load from database or API

        List<Season> seasons = new ArrayList<>();

        try {
            // TMDB ID 66515 (Brothers/Ang Probinsyano) - 9 seasons
            if (entryId == 66515) {
                Log.d(TAG, "Creating seasons for Brothers (Ang Probinsyano) - 9 seasons");

                for (int seasonNum = 1; seasonNum <= 9; seasonNum++) {
                    Season season = new Season();
                    season.setSeason(seasonNum);
                    season.setSeasonPoster(null); // No poster for now

                    // Create episodes for each season with realistic episode counts
                    List<Episode> episodes = new ArrayList<>();
                    int episodesPerSeason;

                    // More realistic episode distribution
                    switch (seasonNum) {
                        case 1: episodesPerSeason = 200; break;
                        case 2: episodesPerSeason = 220; break;
                        case 3: episodesPerSeason = 240; break;
                        case 4: episodesPerSeason = 260; break;
                        case 5: episodesPerSeason = 250; break;
                        case 6: episodesPerSeason = 260; break;
                        case 7: episodesPerSeason = 270; break;
                        case 8: episodesPerSeason = 280; break;
                        case 9: episodesPerSeason = 250; break; // Current/final season
                        default: episodesPerSeason = 250; break;
                    }

                    // Batch create episodes efficiently
                    for (int episodeNum = 1; episodeNum <= episodesPerSeason; episodeNum++) {
                        Episode episode = createEpisodeQuickly(seasonNum, episodeNum, entryId);
                        episodes.add(episode);

                        // Yield control occasionally to prevent ANR
                        if (episodeNum % 100 == 0) {
                            try {
                                Thread.sleep(1); // Micro-pause to keep UI responsive
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                Log.w(TAG, "Episode creation interrupted");
                                return seasons; // Return what we have so far
                            }
                        }
                    }

                    season.setEpisodes(episodes);
                    seasons.add(season);

                    Log.d(TAG, "Created season " + seasonNum + " with " + episodesPerSeason + " episodes");
                }
            }
            // TMDB ID 215803 (Batang Quiapo) - 3 seasons
            else if (entryId == 215803) {
                Log.d(TAG, "Creating seasons for Batang Quiapo - 3 seasons");

                for (int seasonNum = 1; seasonNum <= 3; seasonNum++) {
                    Season season = new Season();
                    season.setSeason(seasonNum);
                    season.setSeasonPoster(null);

                    // Create episodes for each season
                    List<Episode> episodes = new ArrayList<>();
                    int episodesPerSeason;

                    // Realistic episode distribution for Batang Quiapo
                    switch (seasonNum) {
                        case 1: episodesPerSeason = 197; break;
                        case 2: episodesPerSeason = 377; break;
                        case 3: episodesPerSeason = 87; break; // Current season
                        default: episodesPerSeason = 100; break;
                    }

                    // Batch create episodes efficiently
                    for (int episodeNum = 1; episodeNum <= episodesPerSeason; episodeNum++) {
                        Episode episode = createEpisodeQuickly(seasonNum, episodeNum, entryId);
                        episodes.add(episode);

                        // Yield control occasionally
                        if (episodeNum % 50 == 0) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return seasons;
                            }
                        }
                    }

                    season.setEpisodes(episodes);
                    seasons.add(season);

                    Log.d(TAG, "Created season " + seasonNum + " with " + episodesPerSeason + " episodes");
                }
            }

            Log.d(TAG, "Successfully created " + seasons.size() + " seasons for entry " + entryId);

        } catch (Exception e) {
            Log.e(TAG, "Error creating seasons for large series: " + e.getMessage(), e);
        }

        return seasons;
    }

    /**
     * Efficiently create a single episode with minimal object allocation
     */
    private Episode createEpisodeQuickly(int seasonNum, int episodeNum, int entryId) {
        Episode episode = new Episode();
        episode.setEpisode(episodeNum);
        episode.setTitle("Episode " + episodeNum);
        episode.setDescription("Episode " + episodeNum + " of Season " + seasonNum);
        episode.setDuration(entryId == 66515 ? "45 min" : "30 min"); // Different durations per series
        episode.setThumbnail(null); // No thumbnails for performance

        // Create servers efficiently - reuse server objects when possible
        List<Server> servers = new ArrayList<>(1); // Pre-size for single server
        Server server = new Server();
        server.setName("VidSrc Server");
        server.setUrl("https://vidsrc.net/embed/tv/" + entryId + "/" + seasonNum + "/" + episodeNum);
        servers.add(server);

        episode.setServers(servers);
        return episode;
    }

    // Static method to start DetailsActivity
    public static void start(android.content.Context context, Entry entry) {
        try {
            android.content.Intent intent = new android.content.Intent(context, DetailsActivity.class);

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
                Log.w("DetailsActivity", "Large series detected with " + totalEpisodes + " episodes. Using lightweight data transfer.");

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

                Gson gson = new Gson();
                intent.putExtra("entry", gson.toJson(lightweightEntry));
                intent.putExtra("needsFullData", true);
                intent.putExtra("entryId", entry.getId());
            } else {
                // For smaller series, pass the full entry
                Gson gson = new Gson();
                intent.putExtra("entry", gson.toJson(entry));
            }

            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("DetailsActivity", "Error starting DetailsActivity: " + e.getMessage(), e);
            Toast.makeText(context, "Error opening content details", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Fragment handles its own lifecycle
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fragment handles its own lifecycle
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Fragment handles its own cleanup
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            // FullScreenActivity finished
            isInFullscreen = false;

            if (resultCode == RESULT_OK && data != null) {
                // Handle any result data if needed
                long finalPosition = data.getLongExtra("final_position", 0);
                boolean wasPlaying = data.getBooleanExtra("was_playing", false);

                Log.d(TAG, "Video player finished - Position: " + finalPosition + ", Was playing: " + wasPlaying);
            }
        }
    }

    // CinemaX-style dialog and action methods

    private void showServerSelectionDialog() {
        if (currentServers == null || currentServers.isEmpty()) {
            Toast.makeText(this, "No servers available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove auto-play logic - always show dialog for user selection
        // if (currentServers.size() == 1) {
        //     // If only one server, play directly
        //     playServer(0);
        //     return;
        // }

        // Create bottom dialog similar to CinemaX
        serverSelectionDialog = new Dialog(this, android.R.style.Theme_Dialog);
        serverSelectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        serverSelectionDialog.setCancelable(true);
        serverSelectionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Window window = serverSelectionDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        window.setAttributes(wlp);

        serverSelectionDialog.setContentView(R.layout.dialog_server_selection);

        RelativeLayout dialogCloseArea = serverSelectionDialog.findViewById(R.id.relative_layout_dialog_server_close);
        RecyclerView serverRecyclerView = serverSelectionDialog.findViewById(R.id.recycle_view_dialog_servers);

        this.linearLayoutManagerServers = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        ServerSelectionAdapter serverAdapter = new ServerSelectionAdapter();
        //serverRecyclerView.setHasFixedSize(true);
        serverRecyclerView.setAdapter(serverAdapter);
        serverRecyclerView.setLayoutManager(linearLayoutManagerServers);

        dialogCloseArea.setOnClickListener(v -> serverSelectionDialog.dismiss());

        serverSelectionDialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    serverSelectionDialog.dismiss();
                }
                return true;
            }
        });

        serverSelectionDialog.show();
    }

    private void playServer(int serverIndex) {
        if (currentServers != null && serverIndex >= 0 && serverIndex < currentServers.size()) {
            Server selectedServer = currentServers.get(serverIndex);

            // Update current server index
            currentServerIndex = serverIndex;

            // Stop any existing video player before starting new one
            stopCurrentVideoPlayer();

            // Load the video using existing player infrastructure
            loadVideoFromServer(selectedServer);

            if (serverSelectionDialog != null) {
                serverSelectionDialog.dismiss();
            }
        }
    }

    private void stopCurrentVideoPlayer() {
        // Send broadcast to stop any existing FullScreenActivity
        Intent stopIntent = new Intent("STOP_VIDEO_PLAYER");
        sendBroadcast(stopIntent);

        // Reset fullscreen state
        isInFullscreen = false;

        // Small delay to ensure the broadcast is processed
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loadVideoFromServer(Server server) {
        try {
            // Set fullscreen state to true
            isInFullscreen = true;

            String videoUrl = VideoServerUtils.enhanceVideoUrl(server.getUrl());

            // Prepare servers JSON for in-player switcher
            String serversJson = null;
            if (currentServers != null && !currentServers.isEmpty()) {
                serversJson = new Gson().toJson(currentServers);
            }

            // Check if this is an MPD file that requires Shaka Player
            if (VideoServerUtils.isMpdUrl(videoUrl)) {
                Log.d(TAG, "Detected MPD stream, using EmbedActivity with Shaka Player");

                // Use EmbedActivity directly for MPD files
                if (server.hasLicense()) {
                    Log.d(TAG, "License provided: " + server.getLicense() +
                          " (DRM: " + server.isDrmProtected() + ")");
                    if (serversJson != null) {
                        EmbedActivity.startWithServers(this, videoUrl, server.getLicense(), server.isDrmProtected(), null, new ArrayList<>(currentServers), currentServerIndex);
                    } else {
                        EmbedActivity.startWithConfig(this, videoUrl, server.getLicense(), server.isDrmProtected(), null);
                    }
                } else {
                    if (serversJson != null) {
                        EmbedActivity.startWithServers(this, videoUrl, null, false, null, new ArrayList<>(currentServers), currentServerIndex);
                    } else {
                        EmbedActivity.start(this, videoUrl);
                    }
                }

                String drmStatus = server.isDrmProtected() ? " (DRM)" :
                                 (server.hasLicense() ? " (Auth)" : " (Free)");
                Log.d(TAG, "Loading MPD stream from server: " + server.getName() + " - " + server.getUrl() + drmStatus);
                Toast.makeText(this, "Loading " + server.getName() + drmStatus, Toast.LENGTH_SHORT).show();
            } else {
                // Use FullScreenActivity for non-MPD content
                if (serversJson != null) {
                    FullScreenActivity.startWithServers(this, videoUrl, 0, true, currentServerIndex, serversJson, null);
                } else {
                    FullScreenActivity.start(this, videoUrl, 0, true, currentServerIndex, null);
                }

                Log.d(TAG, "Loading video from server: " + server.getName() + " - " + server.getUrl());
                Toast.makeText(this, "Loading from " + server.getName(), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading video from server: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading video: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isInFullscreen = false;
        }
    }

    // Removed toggleMyList and shareMovie methods - UI components no longer exist

    // Server Selection Adapter for the dialog
    public class ServerSelectionAdapter extends RecyclerView.Adapter<ServerSelectionAdapter.ServerHolder> {

        @Override
        public ServerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server_spinner, parent, false);
            return new ServerHolder(v);
        }

        @Override
        public void onBindViewHolder(ServerHolder holder, final int position) {
            Server server = currentServers.get(position);

            // Set server name
            holder.textViewServerName.setText(server.getName() != null ? server.getName() : "Server " + (position + 1));

            // Set server info (quality/type info)
            String serverInfo = "Auto Quality";
            // TODO: Add quality field to Server model if needed
            holder.textViewServerInfo.setText(serverInfo);

            // Set quality badge
            String quality = "HD";
            // TODO: Determine quality from server URL or add field to model
            holder.textViewServerQuality.setText(quality);

            // Set server type icon
            if (VideoServerUtils.isEmbeddedVideoUrl(server.getUrl())) {
                holder.imageViewServerTypeIcon.setImageResource(R.drawable.ic_movie);
            } else {
                holder.imageViewServerTypeIcon.setImageResource(R.drawable.ic_media_play);
            }

            // Show download icon only for direct file URLs (integrated in action cluster)
            if (VideoServerUtils.isDirectFileUrl(server.getUrl())) {
                holder.imageViewServerDownload.setVisibility(View.VISIBLE);
                holder.imageViewServerDownload.setOnClickListener(v -> startDirectDownloadForServer(server));
            } else {
                holder.imageViewServerDownload.setVisibility(View.GONE);
            }

            // Show premium indicator if needed
            holder.imageViewServerPremium.setVisibility(View.GONE);

            // Set click listener
            holder.imageViewServerPlay.setOnClickListener(v -> {
                // Use the proper playServer method instead of direct launch
                playServer(position);
            });
        }

        @Override
        public int getItemCount() {
            return currentServers != null ? currentServers.size() : 0;
        }

        public class ServerHolder extends RecyclerView.ViewHolder {
            ImageView imageViewServerTypeIcon;
            ImageView imageViewServerPremium;
            TextView textViewServerName;
            TextView textViewServerInfo;
            TextView textViewServerQuality;
            ImageView imageViewServerPlay;
            ImageView imageViewServerDownload;

            public ServerHolder(View itemView) {
                super(itemView);
                imageViewServerTypeIcon = itemView.findViewById(R.id.image_view_server_type_icon);
                imageViewServerPremium = itemView.findViewById(R.id.image_view_server_premium);
                textViewServerName = itemView.findViewById(R.id.text_view_server_name);
                textViewServerInfo = itemView.findViewById(R.id.text_view_server_info);
                textViewServerQuality = itemView.findViewById(R.id.text_view_server_quality);
                imageViewServerPlay = itemView.findViewById(R.id.image_view_server_play);
                imageViewServerDownload = itemView.findViewById(R.id.image_view_server_download);
            }
        }
    }



    private void setupRelatedContent() {
        if (currentEntry == null) return;

        // Initialize the repository
        DataRepository dataRepository = new DataRepository(this);

        // Get related content based on country and category
        dataRepository.getPaginatedFilteredData(
            null, // No genre filter (Entry doesn't have getGenre method)
            currentEntry.getCountry(), // Filter by same country
            null, // No year filter
            0, // First page
            10, // Show 10 related items
            new DataRepository.PaginatedDataCallback() {
                @Override
                public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                    // Filter out the current entry from related content
                    List<Entry> relatedEntries = new ArrayList<>();
                    for (Entry entry : entries) {
                        if (entry.getId() != currentEntry.getId()) { // Compare int IDs
                            relatedEntries.add(entry);
                        }
                    }

                    // Limit to 8 items max for UI performance
                    if (relatedEntries.size() > 8) {
                        relatedEntries = relatedEntries.subList(0, 8);
                    }

                    // Setup the related content RecyclerView
                    setupRelatedContentRecyclerView(relatedEntries);
                }

                @Override
                public void onError(String error) {
                    Log.w(TAG, "Failed to load related content: " + error);
                    // Hide related content section if there's an error
                    LinearLayout relatedSection = findViewById(R.id.linear_layout_activity_movie_more_movies);
                    if (relatedSection != null) {
                        relatedSection.setVisibility(View.GONE);
                    }
                }
            }
        );
    }

    private void setupRelatedContentRecyclerView(List<Entry> relatedEntries) {
        if (relatedEntries == null || relatedEntries.isEmpty()) {
            // Hide related content section if no related items
            LinearLayout relatedSection = findViewById(R.id.linear_layout_activity_movie_more_movies);
            if (relatedSection != null) {
                relatedSection.setVisibility(View.GONE);
            }
            return;
        }

        // Show related content section
        LinearLayout relatedSection = findViewById(R.id.linear_layout_activity_movie_more_movies);
        if (relatedSection != null) {
            relatedSection.setVisibility(View.VISIBLE);
        }

        // Setup the main related content RecyclerView
        RecyclerView relatedRecyclerView = findViewById(R.id.recycle_view_activity_activity_movie_more_movies);
        if (relatedRecyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            relatedRecyclerView.setLayoutManager(layoutManager);

            // Create adapter for related content
            MovieAdapter relatedAdapter = new MovieAdapter(this, relatedEntries, false); // Grid view = false for horizontal
            relatedRecyclerView.setAdapter(relatedAdapter);
        }

        // Hide the legacy RecyclerView to avoid duplication
        if (relatedContentRecyclerView != null) {
            relatedContentRecyclerView.setVisibility(View.GONE);
        }
    }

    private void downloadEpisode(Episode episode) {
        if (episode == null || episode.getServers() == null || episode.getServers().isEmpty()) {
            Toast.makeText(this, "No download sources available", Toast.LENGTH_SHORT).show();
            return;
        }
        // Find the first direct file URL server
        for (Server s : episode.getServers()) {
            if (VideoServerUtils.isDirectFileUrl(s.getUrl())) {
                startDirectDownloadForServer(s);
                return;
            }
        }
        Toast.makeText(this, "No direct download link found for this episode", Toast.LENGTH_SHORT).show();
    }

    private void startDirectDownloadForServer(Server server) {
        if (server == null || server.getUrl() == null) return;
        if (!VideoServerUtils.isDirectFileUrl(server.getUrl())) {
            Toast.makeText(this, "Only direct links can be downloaded", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = currentEntry != null ? currentEntry.getTitle() : (currentEpisode != null ? currentEpisode.getTitle() : "Video");
        long id = com.cinecraze.free.utils.DownloadManagerHelper.enqueueDownload(this, title, server.getUrl());
        if (id > 0) {
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
            // Switch to downloads tab if coming from FragmentMainActivity
            try {
                Intent i = new Intent(this, FragmentMainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                i.putExtra("initial_tab", 4);
                startActivity(i);
            } catch (Exception ignored) {}
        } else {
            Toast.makeText(this, "Failed to start download", Toast.LENGTH_SHORT).show();
        }
    }
}