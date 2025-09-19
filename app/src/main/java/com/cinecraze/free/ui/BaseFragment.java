package com.cinecraze.free.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.cinecraze.free.CarouselAdapter;
import com.cinecraze.free.FilterSpinner;
import com.cinecraze.free.FragmentMainActivity;
import com.cinecraze.free.MovieAdapter;
import com.cinecraze.free.R;
import com.cinecraze.free.models.Entry;
import com.cinecraze.free.repository.DataRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseFragment extends Fragment {

    protected RecyclerView recyclerView;
    protected SwipeRefreshLayout swipeRefreshLayout;
    protected MovieAdapter movieAdapter;
    protected List<Entry> currentPageEntries = new ArrayList<>();
    protected ViewPager2 carouselViewPager;
    protected CarouselAdapter carouselAdapter;
    protected FloatingActionButton fabViewMode;
    
    // Floating Pagination Layout
    protected LinearLayout floatingPaginationLayout;
    protected ImageView btnPreviousPage;
    protected ImageView btnNextPage;
    
    // Filter UI elements
    protected MaterialButton btnGenreFilter;
    protected MaterialButton btnCountryFilter;
    protected MaterialButton btnYearFilter;
    protected FilterSpinner genreSpinner;
    protected FilterSpinner countrySpinner;
    protected FilterSpinner yearSpinner;
    
    // Filter variables
    protected String currentGenreFilter = null;
    protected String currentCountryFilter = null;
    protected String currentYearFilter = null;
    protected boolean isLoading = false;

    protected boolean isGridView = true;
    protected DataRepository dataRepository;
    
    // Pagination variables
    protected int currentPage = 0;
    protected int pageSize = 20;
    protected boolean hasMorePages = false;
    protected int totalCount = 0;
    protected String currentCategory = "";
    protected String currentSearchQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentCategory = getCategory();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataRepository = new DataRepository(getContext());
        initializeViews(view);
        setupRecyclerView();
        setupCarousel();
        setupViewSwitch();
        setupPagination();
        setupFilters();
        setupSwipeRefresh();
        loadInitialData();
    }

    protected abstract void initializeViews(View view);
    protected abstract String getCategory();
    protected abstract int getLayoutId();

    protected void setupRecyclerView() {
        if (recyclerView != null) {
            movieAdapter = new MovieAdapter(getContext(), currentPageEntries, isGridView);
            updateViewMode();
            
            // Add scroll listener to show pagination only when scrolling to bottom and to hide/show bottom navigation
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                private int lastDy = 0;

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    // Check if user has scrolled to bottom and there are 20+ items
                    if (!recyclerView.canScrollVertically(1) && totalCount > 20) {
                        // User has scrolled to bottom and there are more than 20 items
                        if (floatingPaginationLayout != null) {
                            floatingPaginationLayout.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // Hide pagination when not at bottom
                        if (floatingPaginationLayout != null) {
                            floatingPaginationLayout.setVisibility(View.GONE);
                        }
                    }

                    // Hide/show bottom navigation
                    if (getActivity() instanceof FragmentMainActivity) {
                        FragmentMainActivity activity = (FragmentMainActivity) getActivity();
                        if (dy > 0 && lastDy <= 0) {
                            // Scrolling down
                            activity.hideBottomNavigation();
                        } else if (dy < 0 && lastDy > 0) {
                            // Scrolling up
                            activity.showBottomNavigation();
                        }
                    }
                    lastDy = dy;
                }
            });
        }
    }

    protected void setupCarousel() {
        if (carouselViewPager != null) {
            carouselAdapter = new CarouselAdapter(getContext(), new ArrayList<>());
            carouselViewPager.setAdapter(carouselAdapter);

            // Auto-scroll handler
            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int currentItem = carouselViewPager.getCurrentItem();
                    int totalItems = carouselAdapter.getItemCount();
                    if (totalItems > 0) {
                        carouselViewPager.setCurrentItem((currentItem + 1) % totalItems, true);
                    }
                    handler.postDelayed(this, 3000); // 3 seconds delay
                }
            };
            handler.postDelayed(runnable, 3000);
        }
    }

    protected void updateViewMode() {
        if (recyclerView != null && movieAdapter != null) {
            if (isGridView) {
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                if (fabViewMode != null) fabViewMode.setImageResource(R.drawable.ic_grid_view);
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                if (fabViewMode != null) fabViewMode.setImageResource(R.drawable.ic_list_view);
            }
            movieAdapter.setGridView(isGridView);
            recyclerView.setAdapter(movieAdapter);
        }
    }

    protected void setupViewSwitch() {
        if (fabViewMode != null) {
            fabViewMode.setOnClickListener(v -> {
                isGridView = !isGridView;
                updateViewMode();
            });
        }
    }

    protected void setupPagination() {
        if (btnPreviousPage != null) {
            btnPreviousPage.setOnClickListener(v -> {
                if (currentPage > 0) {
                    currentPage--;
                    loadPageData();
                }
            });
        }

        if (btnNextPage != null) {
            btnNextPage.setOnClickListener(v -> {
                if (hasMorePages) {
                    currentPage++;
                    loadPageData();
                }
            });
        }
    }

    protected void setupFilters() {
        if (btnGenreFilter != null && btnCountryFilter != null && btnYearFilter != null) {
            // Initialize filter spinners only if they haven't been created yet
            if (genreSpinner == null) {
                genreSpinner = new FilterSpinner(getContext(), "Genre", new ArrayList<>(), currentGenreFilter);
                countrySpinner = new FilterSpinner(getContext(), "Country", new ArrayList<>(), currentCountryFilter);
                yearSpinner = new FilterSpinner(getContext(), "Year", new ArrayList<>(), currentYearFilter);

                // Common filter selection listener
                FilterSpinner.OnFilterSelectedListener filterListener = (filterType, filterValue) -> {
                    switch (filterType) {
                        case "Genre":
                            currentGenreFilter = filterValue;
                            btnGenreFilter.setText(filterValue != null ? filterValue : "Genre");
                            break;
                        case "Country":
                            currentCountryFilter = filterValue;
                            btnCountryFilter.setText(filterValue != null ? filterValue : "Country");
                            break;
                        case "Year":
                            currentYearFilter = filterValue;
                            btnYearFilter.setText(filterValue != null ? filterValue : "Year");
                            break;
                    }

                    // Reset pagination and apply filters
                    currentPage = 0;
                    currentSearchQuery = ""; // Clear search when filtering
                    loadPageData();
                };

                genreSpinner.setOnFilterSelectedListener(filterListener);
                countrySpinner.setOnFilterSelectedListener(filterListener);
                yearSpinner.setOnFilterSelectedListener(filterListener);

                // Set up button click listeners to show spinners
                btnGenreFilter.setOnClickListener(v -> {
                    populateFilterSpinners();
                    dismissAllSpinners();
                    genreSpinner.show(btnGenreFilter);
                });

                btnCountryFilter.setOnClickListener(v -> {
                    populateFilterSpinners();
                    dismissAllSpinners();
                    countrySpinner.show(btnCountryFilter);
                });

                btnYearFilter.setOnClickListener(v -> {
                    populateFilterSpinners();
                    dismissAllSpinners();
                    yearSpinner.show(btnYearFilter);
                });
            }
        }
    }
    
    protected void populateFilterSpinners() {
        if (dataRepository == null) return;
        
        // Get unique values from repository and populate spinners
        List<String> genres = dataRepository.getUniqueGenres();
        List<String> countries = dataRepository.getUniqueCountries();
        List<String> years = dataRepository.getUniqueYears();
        
        if (genreSpinner != null) {
            genreSpinner.updateFilterValues(genres);
        }
        if (countrySpinner != null) {
            countrySpinner.updateFilterValues(countries);
        }
        if (yearSpinner != null) {
            yearSpinner.updateFilterValues(years);
        }
    }
    
    protected void dismissAllSpinners() {
        if (genreSpinner != null && genreSpinner.isShowing()) {
            genreSpinner.dismiss();
        }
        if (countrySpinner != null && countrySpinner.isShowing()) {
            countrySpinner.dismiss();
        }
        if (yearSpinner != null && yearSpinner.isShowing()) {
            yearSpinner.dismiss();
        }
    }

    protected void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                currentPage = 0;
                currentSearchQuery = "";
                // Force fetch latest data from API, then reload the current page from cache
                if (dataRepository != null) {
                    dataRepository.forceRefreshData(new DataRepository.DataCallback() {
                        @Override
                        public void onSuccess(List<Entry> entries) {
                            // After cache is updated, reload paginated data
                            loadPageData();
                            // updatePageData() will stop the refreshing indicator
                        }

                        @Override
                        public void onError(String error) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (swipeRefreshLayout != null) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                    Toast.makeText(getContext(), "Refresh failed: " + error, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    });
                } else {
                    // Fallback: just reload current page
                    loadPageData();
                }
            });
        }
    }

    protected void loadInitialData() {
        currentPage = 0;
        
        // Ensure data is available before loading
        dataRepository.ensureDataAvailable(new DataRepository.DataCallback() {
            @Override
            public void onSuccess(List<Entry> entries) {
                loadPageData();
                populateFilterSpinners(); // Populate filter spinners after data is loaded
                // After initial load, check in background if newer data exists and update UI if so
                triggerBackgroundRefreshIfNeeded();
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Failed to initialize: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    protected void loadPageData() {
        if (getActivity() == null) return;
        
        isLoading = true;
        
        // Use DataRepository's paginated methods for better performance
        if (hasActiveFilters()) {
            loadFilteredPageData();
        } else if (!currentSearchQuery.isEmpty()) {
            loadSearchPageData();
        } else if (!currentCategory.isEmpty()) {
            loadCategoryPageData();
        } else {
            loadAllPageData();
        }
    }
    
    protected boolean hasActiveFilters() {
        return currentGenreFilter != null || currentCountryFilter != null || currentYearFilter != null;
    }
    
    protected void loadAllPageData() {
        dataRepository.getPaginatedData(currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                updatePageData(entries, hasMorePages, totalCount);
            }
            
            @Override
            public void onError(String error) {
                handlePageLoadError(error);
            }
        });
    }
    
    protected void loadCategoryPageData() {
        dataRepository.getPaginatedDataByCategory(currentCategory, currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                updatePageData(entries, hasMorePages, totalCount);
            }
            
            @Override
            public void onError(String error) {
                handlePageLoadError(error);
            }
        });
    }
    
    protected void loadSearchPageData() {
        dataRepository.searchPaginated(currentSearchQuery, currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                updatePageData(entries, hasMorePages, totalCount);
            }
            
            @Override
            public void onError(String error) {
                handlePageLoadError(error);
            }
        });
    }
    
    protected void loadFilteredPageData() {
        dataRepository.getPaginatedFilteredData(currentGenreFilter, currentCountryFilter, currentYearFilter, 
                currentPage, pageSize, new DataRepository.PaginatedDataCallback() {
            @Override
            public void onSuccess(List<Entry> entries, boolean hasMorePages, int totalCount) {
                updatePageData(entries, hasMorePages, totalCount);
            }
            
            @Override
            public void onError(String error) {
                handlePageLoadError(error);
            }
        });
    }
    
    protected void updatePageData(List<Entry> entries, boolean hasMorePages, int totalCount) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            this.hasMorePages = hasMorePages;
            this.totalCount = totalCount;
            this.isLoading = false;
            
            currentPageEntries.clear();
            currentPageEntries.addAll(entries);
            
            if (movieAdapter != null) {
                movieAdapter.setEntryList(currentPageEntries);
            }
            updatePaginationUI();
            
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            
            // Load carousel data if this is the first page and home fragment
            if (currentPage == 0 && carouselAdapter != null && getCategory().isEmpty()) {
                List<Entry> topRatedEntries = dataRepository.getTopRatedEntries(10);
                carouselAdapter.setEntries(topRatedEntries);
                carouselAdapter.notifyDataSetChanged();
            }
        });
    }
    
    protected void handlePageLoadError(String error) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            isLoading = false;
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getContext(), "Failed to load page: " + error, Toast.LENGTH_SHORT).show();
        });
    }
    
    protected void loadPageDataOld() {
        if (getActivity() == null) return;
        
        new Thread(() -> {
            try {
                List<Entry> allEntries;
                
                if (currentSearchQuery.isEmpty()) {
                    if (currentCategory.isEmpty()) {
                        allEntries = dataRepository.getAllCachedEntries();
                    } else {
                        allEntries = dataRepository.getEntriesByCategory(currentCategory);
                    }
                } else {
                    allEntries = dataRepository.searchByTitle(currentSearchQuery);
                }
                
                // Calculate pagination
                final int totalItems = allEntries.size();
                final int startIndex = currentPage * pageSize;
                final int endIndex = Math.min(startIndex + pageSize, totalItems);
                
                final List<Entry> pageEntries = new ArrayList<>();
                if (startIndex < totalItems) {
                    pageEntries.addAll(allEntries.subList(startIndex, endIndex));
                }
                
                final boolean hasMore = endIndex < totalItems;
                
                // Prepare carousel data if needed
                final List<Entry> carouselEntries = new ArrayList<>();
                if (currentPage == 0 && !pageEntries.isEmpty()) {
                    int carouselSize = Math.min(5, pageEntries.size());
                    for (int i = 0; i < carouselSize; i++) {
                        carouselEntries.add(pageEntries.get(i));
                    }
                }
                
                // Update UI on main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentPageEntries.clear();
                        currentPageEntries.addAll(pageEntries);
                        if (movieAdapter != null) {
                            movieAdapter.notifyDataSetChanged();
                        }
                        
                        totalCount = totalItems;
                        hasMorePages = hasMore;
                        updatePaginationButtons();
                        
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        
                        // Load carousel data if this is the first page
                        if (currentPage == 0 && carouselAdapter != null && !carouselEntries.isEmpty()) {
                            carouselAdapter.setEntries(carouselEntries);
                            carouselAdapter.notifyDataSetChanged();
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        Toast.makeText(getContext(), "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    protected void updatePaginationUI() {
        // Only update button states, not visibility (visibility is controlled by scroll listener)
        if (btnPreviousPage != null && btnNextPage != null) {
            boolean canGoPrevious = currentPage > 0 && !isLoading;
            boolean canGoNext = hasMorePages && !isLoading && ((currentPage + 1) * pageSize < totalCount);
            
            btnPreviousPage.setEnabled(canGoPrevious);
            btnPreviousPage.setAlpha(canGoPrevious ? 1.0f : 0.3f);
            
            btnNextPage.setEnabled(canGoNext);
            btnNextPage.setAlpha(canGoNext ? 1.0f : 0.3f);
        }
    }
    
    protected void updatePaginationButtons() {
        updatePaginationUI();
    }

    protected void filterByQuery(String query) {
        currentSearchQuery = query;
        currentPage = 0;
        loadPageData();
    }

    // Public method to be called from MainActivity for search
    public void performSearch(String query) {
        filterByQuery(query);
    }

    private void triggerBackgroundRefreshIfNeeded() {
        if (dataRepository == null) return;
        final int beforeCount = dataRepository.getTotalEntriesCount();
        dataRepository.forceRefreshData(new DataRepository.DataCallback() {
            @Override
            public void onSuccess(List<Entry> entries) {
                int afterCount = dataRepository.getTotalEntriesCount();
                if (afterCount != beforeCount && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        currentPage = 0;
                        loadPageData();
                    });
                }
            }

            @Override
            public void onError(String error) {
                // Silent fail; keep cached data
            }
        });
    }
}