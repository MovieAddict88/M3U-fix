package com.cinecraze.free;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cinecraze.free.R;

import java.util.ArrayList;
import java.util.List;

public class FilterSpinner {

    private PopupWindow popupWindow;
    private List<String> filterValues;
    private OnFilterSelectedListener listener;
    private String currentFilter;
    private String filterType;
    private Context context;

    public interface OnFilterSelectedListener {
        void onFilterSelected(String filterType, String filterValue);
    }

    public FilterSpinner(Context context, String filterType, List<String> filterValues, String currentFilter) {
        this.context = context;
        this.filterType = filterType;
        this.filterValues = new ArrayList<>();
        this.filterValues.add("All " + filterType); // Add "All" option
        if (filterValues != null) {
            this.filterValues.addAll(filterValues);
        }
        this.currentFilter = currentFilter;
        createFilterPopupWindow();
    }

    private void createFilterPopupWindow() {
        // Create ListView for filter options
        ListView listView = new ListView(context);
        
        // Create background with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#2C2C2C"));
        background.setCornerRadius(12f);
        background.setStroke(1, Color.parseColor("#404040"));
        listView.setBackground(background);
        
        // Remove dividers for cleaner look
        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setPadding(8, 8, 8, 8);
        
        // Create adapter with custom layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                R.layout.item_filter_spinner, R.id.filter_name, filterValues) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(R.id.filter_name);

                String filterValue = getItem(position);
                boolean isSelected = (currentFilter == null && position == 0) ||
                        (currentFilter != null && currentFilter.equals(filterValue));

                if (isSelected) {
                    textView.setTextColor(Color.parseColor("#FF6B35"));
                } else {
                    textView.setTextColor(Color.parseColor("#FFFFFF"));
                }

                return view;
            }
        };

        listView.setAdapter(adapter);

        // Handle item clicks
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null && position < filterValues.size()) {
                String selectedValue = filterValues.get(position);
                // If "All" is selected, pass null to clear filter
                String filterValue = position == 0 ? null : selectedValue;
                listener.onFilterSelected(filterType, filterValue);
            }
            dismiss();
        });

        // Create PopupWindow with appropriate size
        popupWindow = new PopupWindow(listView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                Math.min(600, filterValues.size() * 48 + 32)); // Max height with item size
        
        // Set background
        GradientDrawable popupBackground = new GradientDrawable();
        popupBackground.setColor(Color.parseColor("#2C2C2C"));
        popupBackground.setCornerRadius(12f);
        popupBackground.setStroke(1, Color.parseColor("#404040"));
        popupWindow.setBackgroundDrawable(popupBackground);
        
        // Add shadow and properties
        popupWindow.setElevation(8f);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
    }

    public void updateFilterValues(List<String> newFilterValues) {
        this.filterValues.clear();
        this.filterValues.add("All " + filterType);
        if (newFilterValues != null) {
            this.filterValues.addAll(newFilterValues);
        }
        createFilterPopupWindow(); // Recreate popup with new values
    }

    public void setOnFilterSelectedListener(OnFilterSelectedListener listener) {
        this.listener = listener;
    }

    public void show(View anchorView) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            // Calculate position to show below the anchor
            int[] location = new int[2];
            anchorView.getLocationInWindow(location);
            
            int x = location[0];
            int y = location[1] + anchorView.getHeight() + 8; // 8dp gap
            
            // Ensure popup doesn't go off screen
            int screenWidth = anchorView.getContext().getResources().getDisplayMetrics().widthPixels;
            int popupWidth = Math.min(300, screenWidth - 32); // Max width with margins
            popupWindow.setWidth(popupWidth);
            
            if (x + popupWidth > screenWidth) {
                x = screenWidth - popupWidth - 16; // 16dp margin from edge
            }
            
            // Show below the anchor view
            popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y);
        }
    }

    public void dismiss() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public void setCurrentFilter(String currentFilter) {
        this.currentFilter = currentFilter;
        createFilterPopupWindow(); // Recreate to update selection
    }
}