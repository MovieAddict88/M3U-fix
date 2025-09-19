package com.cinecraze.free.repository;

import android.content.Context;
import android.util.Log;

import com.cinecraze.free.database.CineCrazeDatabase;
import com.cinecraze.free.database.DatabaseUtils;
import com.cinecraze.free.database.entities.CacheMetadataEntity;
import com.cinecraze.free.database.entities.EntryEntity;
import com.cinecraze.free.models.Category;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.models.Playlist;
import com.cinecraze.free.models.PlaylistsVersion;
import com.cinecraze.free.net.ApiService;
import com.cinecraze.free.net.RetrofitClient;

import java.util.ArrayList;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataRepository {

    private static final String TAG = "DataRepository";
    private static final String CACHE_KEY_PLAYLIST = "playlist_data";
    private static final String CACHE_KEY_PLAYLIST_VERSION = "playlist_version";
    private static final long CACHE_EXPIRY_HOURS = 24; // Cache expires after 24 hours
    public static final int DEFAULT_PAGE_SIZE = 20; // Default items per page

    private CineCrazeDatabase database;
    private ApiService apiService;
    private Handler mainHandler;


    public interface DataCallback {
        void onSuccess(List<Entry> entries);
        void onError(String error);
    }

    public interface UpdateCheckCallback {
        void onUpdateAvailable(PlaylistsVersion newVersion);
        void onNoUpdate();
        void onError(String error);
    }

    public interface PaginatedDataCallback {
        void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount);
        void onError(String error);
    }

    public DataRepository(Context context) {
        database = CineCrazeDatabase.getInstance(context);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Expose cache validity so UI can decide whether to prompt before downloading
     */
    public boolean hasValidCache() {
        try {
            CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST);
            return metadata != null && isCacheValid(metadata.getLastUpdated());
        } catch (Exception e) {
            Log.e(TAG, "Error checking cache validity: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get playlist data - checks cache first, then fetches from API if needed
     */
    public void getPlaylistData(DataCallback callback) {
        checkForUpdates(new UpdateCheckCallback() {
            @Override
            public void onUpdateAvailable(PlaylistsVersion newVersion) {
                downloadPlaylists(newVersion, callback);
            }

            @Override
            public void onNoUpdate() {
                callback.onSuccess(new ArrayList<>());
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Force refresh data from API (ignores cache)
     * This method is used for pull-to-refresh functionality
     */
    public void forceRefreshData(DataCallback callback) {
        Log.d(TAG, "Force refreshing data from API");
        getPlaylistData(callback);
    }

    /**
     * Check if data is available in cache and initialize if needed
     * This method only loads data if cache is empty, otherwise just confirms cache exists
     */
    public void ensureDataAvailable(DataCallback callback) {
        CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST_VERSION);

        if (metadata != null && isCacheValid(metadata.getLastUpdated())) {
            // Cache exists and is valid - just return success without loading all data
            Log.d(TAG, "Cache is available and valid - ready for pagination");
            callback.onSuccess(new ArrayList<>()); // Empty list, pagination will load actual data
        } else {
            // No valid cache - need to fetch all data once to populate cache
            Log.d(TAG, "No valid cache - fetching data to populate cache");
            getPlaylistData(callback);
        }
    }

    /**
     * Get paginated data from cache
     */
    public void getPaginatedData(int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().getEntriesPaged(pageSize, offset);
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getEntriesCount();
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Loaded page " + page + " with " + entries.size() + " items. Total: " + totalCount + ", HasMore: " + hasMorePages);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated data: " + e.getMessage(), e);
            callback.onError("Error loading page: " + e.getMessage());
        }
    }

    /**
     * Get paginated data by category
     */
    public void getPaginatedDataByCategory(String category, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().getEntriesByCategoryPaged(category, pageSize, offset);
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getEntriesCountByCategory(category);
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Loaded category '" + category + "' page " + page + " with " + entries.size() + " items. Total: " + totalCount);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated category data: " + e.getMessage(), e);
            callback.onError("Error loading category page: " + e.getMessage());
        }
    }

    /**
     * Search with pagination
     */
    public void searchPaginated(String searchQuery, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().searchByTitlePaged(searchQuery, pageSize, offset);
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getSearchResultsCount(searchQuery);
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Search '" + searchQuery + "' page " + page + " with " + entries.size() + " results. Total: " + totalCount);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error searching with pagination: " + e.getMessage(), e);
            callback.onError("Error searching: " + e.getMessage());
        }
    }

    /**
     * Force refresh data from API
     */
    public void refreshData(DataCallback callback) {
        Log.d(TAG, "Force refreshing data from API");
        getPlaylistData(callback);
    }

    /**
     * Get entries by category from cache
     */
    public List<Entry> getEntriesByCategory(String category) {
        List<EntryEntity> entities = database.entryDao().getEntriesByCategory(category);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    /**
     * Search entries by title from cache
     */
    public List<Entry> searchByTitle(String title) {
        List<EntryEntity> entities = database.entryDao().searchByTitle(title);
        return DatabaseUtils.entitiesToEntries(entities);
    }

    /**
     * Get all cached entries
     */
    public List<Entry> getAllCachedEntries() {
        List<EntryEntity> entities = database.entryDao().getAllEntries();
        return DatabaseUtils.entitiesToEntries(entities);
    }

    /**
     * Get total count of cached entries
     */
    public int getTotalEntriesCount() {
        return database.entryDao().getEntriesCount();
    }

    /**
     * Get unique genres from cached data
     */
    public List<String> getUniqueGenres() {
        try {
            List<String> genres = database.entryDao().getUniqueGenres();
            // Filter out null and empty values
            List<String> filteredGenres = new ArrayList<>();
            for (String genre : genres) {
                if (genre != null && !genre.trim().isEmpty() && !genre.equalsIgnoreCase("null")) {
                    filteredGenres.add(genre.trim());
                }
            }
            return filteredGenres;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique genres: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unique countries from cached data
     */
    public List<String> getUniqueCountries() {
        try {
            List<String> countries = database.entryDao().getUniqueCountries();
            // Filter out null and empty values
            List<String> filteredCountries = new ArrayList<>();
            for (String country : countries) {
                if (country != null && !country.trim().isEmpty() && !country.equalsIgnoreCase("null")) {
                    filteredCountries.add(country.trim());
                }
            }
            return filteredCountries;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique countries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unique years from cached data
     */
    public List<String> getUniqueYears() {
        try {
            List<String> years = database.entryDao().getUniqueYears();
            // Filter out null, empty, and zero values
            List<String> filteredYears = new ArrayList<>();
            for (String year : years) {
                if (year != null && !year.trim().isEmpty() && !year.equalsIgnoreCase("null") && !year.equals("0")) {
                    filteredYears.add(year.trim());
                }
            }
            return filteredYears;
        } catch (Exception e) {
            Log.e(TAG, "Error getting unique years: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get paginated data filtered by genre, country, and year
     */
    public void getPaginatedFilteredData(String genre, String country, String year, int page, int pageSize, PaginatedDataCallback callback) {
        try {
            int offset = page * pageSize;
            List<EntryEntity> entities = database.entryDao().getEntriesFilteredPaged(
                genre == null || genre.isEmpty() ? null : genre,
                country == null || country.isEmpty() ? null : country,
                year == null || year.isEmpty() ? null : year,
                pageSize, offset
            );
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);
            int totalCount = database.entryDao().getEntriesFilteredCount(
                genre == null || genre.isEmpty() ? null : genre,
                country == null || country.isEmpty() ? null : country,
                year == null || year.isEmpty() ? null : year
            );
            boolean hasMorePages = (offset + pageSize) < totalCount;

            Log.d(TAG, "Loaded filtered page " + page + " with " + entries.size() + " items. Total: " + totalCount);
            callback.onSuccess(entries, hasMorePages, totalCount);
        } catch (Exception e) {
            Log.e(TAG, "Error loading filtered paginated data: " + e.getMessage(), e);
            callback.onError("Error loading filtered page: " + e.getMessage());
        }
    }

    public List<Entry> getTopRatedEntries(int count) {
        try {
            List<EntryEntity> entities = database.entryDao().getTopRatedEntries(count);
            return DatabaseUtils.entitiesToEntries(entities);
        } catch (Exception e) {
            Log.e(TAG, "Error getting top rated entries: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Check if cache is still valid
     */
    private boolean isCacheValid(long lastUpdated) {
        long currentTime = System.currentTimeMillis();
        long cacheAge = currentTime - lastUpdated;
        long expiryTime = TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS);
        return cacheAge < expiryTime;
    }

    /**
     * Load data from local cache
     */
    private void loadFromCache(DataCallback callback) {
        try {
            List<EntryEntity> entities = database.entryDao().getAllEntries();
            List<Entry> entries = DatabaseUtils.entitiesToEntries(entities);

            if (!entries.isEmpty()) {
                callback.onSuccess(entries);
            } else {
                // Cache is empty, fetch from API
                Log.d(TAG, "Cache is empty, fetching from API");
                getPlaylistData(callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading from cache: " + e.getMessage(), e);
            getPlaylistData(callback);
        }
    }

    public void checkForUpdates(UpdateCheckCallback callback) {
        apiService.getPlaylistsVersion().enqueue(new Callback<PlaylistsVersion>() {
            @Override
            public void onResponse(Call<PlaylistsVersion> call, Response<PlaylistsVersion> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PlaylistsVersion playlistsVersion = response.body();
                    CacheMetadataEntity metadata = database.cacheMetadataDao().getMetadata(CACHE_KEY_PLAYLIST_VERSION);
                    int localVersion = metadata != null ? Integer.parseInt(metadata.getDataVersion()) : -1;

                    if (playlistsVersion.getVersion() > localVersion) {
                        callback.onUpdateAvailable(playlistsVersion);
                    } else {
                        Log.d(TAG, "No new version found. Using cached data.");
                        callback.onNoUpdate();
                    }
                } else {
                    Log.e(TAG, "Failed to fetch playlist version: " + response.code());
                    callback.onError("Failed to fetch playlist version: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PlaylistsVersion> call, Throwable t) {
                Log.e(TAG, "Failed to fetch playlist version", t);
                callback.onError("Failed to fetch playlist version: " + t.getMessage());
            }
        });
    }

    public void downloadPlaylists(PlaylistsVersion playlistsVersion, DataCallback callback) {
        List<String> playlistUrls = playlistsVersion.getPlaylists();
        List<Playlist> playlists = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger counter = new AtomicInteger(playlistUrls.size());
        AtomicInteger failedCount = new AtomicInteger(0);

        for (String url : playlistUrls) {
            apiService.getPlaylist(url).enqueue(new Callback<Playlist>() {
                @Override
                public void onResponse(Call<Playlist> call, Response<Playlist> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        playlists.add(response.body());
                    } else {
                        Log.e(TAG, "Failed to fetch playlist: " + url);
                        failedCount.incrementAndGet();
                    }
                    if (counter.decrementAndGet() == 0) {
                        handleAllPlaylistsFetched(playlists, playlistsVersion.getVersion(), failedCount.get(), callback);
                    }
                }

                @Override
                public void onFailure(Call<Playlist> call, Throwable t) {
                    Log.e(TAG, "Failed to fetch playlist: " + url, t);
                    failedCount.incrementAndGet();
                    if (counter.decrementAndGet() == 0) {
                        handleAllPlaylistsFetched(playlists, playlistsVersion.getVersion(), failedCount.get(), callback);
                    }
                }
            });
        }
    }

    private void handleAllPlaylistsFetched(List<Playlist> playlists, int version, int failedCount, DataCallback callback) {
        mainHandler.post(() -> {
            if (failedCount > 0) {
                Log.w(TAG, failedCount + " playlists failed to download.");
                if (playlists.isEmpty()) {
                    callback.onError("Failed to download any playlists.");
                    return;
                }
                // Optionally, inform the user about partial data
            }
            cachePlaylists(playlists, version);
            callback.onSuccess(new ArrayList<>());
        });
    }

    /**
     * Cache the playlist data to local database
     */
    private void cachePlaylists(List<Playlist> playlists, int version) {
        try {
            database.entryDao().deleteAll();
            Set<Integer> entryIds = new HashSet<>();
            List<EntryEntity> entitiesToInsert = new ArrayList<>();

            for (Playlist playlist : playlists) {
                if (playlist.getCategories() != null) {
                    for (Category category : playlist.getCategories()) {
                        if (category != null && category.getEntries() != null) {
                            String mainCategory = category.getMainCategory();
                            for (Entry entry : category.getEntries()) {
                                if (entry != null && entryIds.add(entry.getId())) {
                                    EntryEntity entity = DatabaseUtils.entryToEntity(entry, mainCategory);
                                    entitiesToInsert.add(entity);
                                }
                            }
                        }
                    }
                }
            }

            if (!entitiesToInsert.isEmpty()) {
                database.entryDao().insertAll(entitiesToInsert);
            }

            CacheMetadataEntity metadata = new CacheMetadataEntity(
                    CACHE_KEY_PLAYLIST_VERSION,
                    System.currentTimeMillis(),
                    String.valueOf(version)
            );
            database.cacheMetadataDao().insert(metadata);

            Log.d(TAG, "Data cached successfully: " + entitiesToInsert.size() + " entries");
        } catch (Exception e) {
            Log.e(TAG, "Error caching data: " + e.getMessage(), e);
        }
    }
}