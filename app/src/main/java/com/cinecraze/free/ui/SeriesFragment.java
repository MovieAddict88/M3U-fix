package com.cinecraze.free.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cinecraze.free.R;

public class SeriesFragment extends BaseFragment {

    public static SeriesFragment newInstance() {
        return new SeriesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_series, container, false);
    }

    @Override
    protected void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        fabViewMode = view.findViewById(R.id.fab_view_mode);
        
        // Floating Pagination Layout
        floatingPaginationLayout = view.findViewById(R.id.floating_pagination_layout);
        btnPreviousPage = view.findViewById(R.id.btn_previous_page);
        btnNextPage = view.findViewById(R.id.btn_next_page);
        
        // Filter UI elements
        btnGenreFilter = view.findViewById(R.id.btn_genre_filter);
        btnCountryFilter = view.findViewById(R.id.btn_country_filter);
        btnYearFilter = view.findViewById(R.id.btn_year_filter);
    }

    @Override
    protected String getCategory() {
        return "TV Series";
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_series;
    }
}