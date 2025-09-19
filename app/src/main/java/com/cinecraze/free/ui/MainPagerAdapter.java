package com.cinecraze.free.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter for ViewPager2 to handle swipe navigation between main sections
 */
public class MainPagerAdapter extends FragmentStateAdapter {
    
    private static final int NUM_PAGES = 5;
    
    // Fragment instances for reuse
    private HomeFragment homeFragment;
    private MovieFragment movieFragment;
    private SeriesFragment seriesFragment;
    private LiveTVFragment liveTVFragment;
    private DownloadsFragment downloadsFragment;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                if (homeFragment == null) {
                    homeFragment = HomeFragment.newInstance();
                }
                return homeFragment;
            case 1:
                if (movieFragment == null) {
                    movieFragment = MovieFragment.newInstance();
                }
                return movieFragment;
            case 2:
                if (seriesFragment == null) {
                    seriesFragment = SeriesFragment.newInstance();
                }
                return seriesFragment;
            case 3:
                if (liveTVFragment == null) {
                    liveTVFragment = LiveTVFragment.newInstance();
                }
                return liveTVFragment;
            case 4:
                if (downloadsFragment == null) {
                    downloadsFragment = DownloadsFragment.newInstance();
                }
                return downloadsFragment;
            default:
                if (homeFragment == null) {
                    homeFragment = HomeFragment.newInstance();
                }
                return homeFragment;
        }
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
    
    /**
     * Get the fragment at a specific position for external access
     */
    public Fragment getFragmentAt(int position) {
        switch (position) {
            case 0:
                return homeFragment;
            case 1:
                return movieFragment;
            case 2:
                return seriesFragment;
            case 3:
                return liveTVFragment;
            case 4:
                return downloadsFragment;
            default:
                return homeFragment;
        }
    }
}