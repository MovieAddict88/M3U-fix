package com.cinecraze.free;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cinecraze.free.models.Server;
import com.cinecraze.free.R;

import java.util.List;

public class CompactServerSpinner {

    private PopupWindow popupWindow;
    private List<Server> servers;
    private OnServerSelectedListener listener;
    private int currentServerIndex;

    public interface OnServerSelectedListener {
        void onServerSelected(Server server, int position);
    }

    public CompactServerSpinner(Context context, List<Server> servers, int currentServerIndex) {
        this.servers = servers;
        this.currentServerIndex = currentServerIndex;
        createCompactPopupWindow(context);
    }

    private void createCompactPopupWindow(Context context) {
        // Create ListView for server options with compact design
        ListView listView = new ListView(context);
        
        // Create compact background with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#2C2C2C"));
        background.setCornerRadius(8f);
        background.setStroke(1, Color.parseColor("#404040"));
        listView.setBackground(background);
        
        // Remove dividers for cleaner look
        listView.setDividerHeight(0);
        listView.setDivider(null);
        
        // Create adapter with custom layout
        ArrayAdapter<Server> adapter = new ArrayAdapter<Server>(context, 
                R.layout.item_server_spinner) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(context).inflate(R.layout.item_server_spinner, parent, false);
                }
                
                TextView serverName = view.findViewById(R.id.server_name);
                TextView serverNameNew = view.findViewById(R.id.text_view_server_name);
                View selectedIndicator = view.findViewById(R.id.selected_indicator);
                
                if (position < servers.size()) {
                    Server server = servers.get(position);
                    
                    // Use new layout components if available, fallback to legacy
                    if (serverNameNew != null) {
                        serverNameNew.setText(server.getName());
                    } else if (serverName != null) {
                        serverName.setText(server.getName());
                    }
                    
                    // Show/hide selection indicator
                    if (position == currentServerIndex) {
                        selectedIndicator.setVisibility(View.VISIBLE);
                        view.setSelected(true);
                    } else {
                        selectedIndicator.setVisibility(View.GONE);
                        view.setSelected(false);
                    }
                }
                
                return view;
            }
        };

        // Add servers to adapter
        adapter.addAll(servers);

        listView.setAdapter(adapter);

        // Handle item clicks
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null && position < servers.size()) {
                listener.onServerSelected(servers.get(position), position);
            }
            dismiss();
        });

        // Create compact PopupWindow
        popupWindow = new PopupWindow(listView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Set compact background
        GradientDrawable popupBackground = new GradientDrawable();
        popupBackground.setColor(Color.parseColor("#2C2C2C"));
        popupBackground.setCornerRadius(8f);
        popupBackground.setStroke(1, Color.parseColor("#404040"));
        popupWindow.setBackgroundDrawable(popupBackground);
        
        // Add subtle shadow
        popupWindow.setElevation(4f);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        
        // Set animation
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
    }

    public void setOnServerSelectedListener(OnServerSelectedListener listener) {
        this.listener = listener;
    }

    public void show(View anchorView) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            // Calculate position to show below the anchor
            int[] location = new int[2];
            anchorView.getLocationInWindow(location);
            
            int x = location[0];
            int y = location[1] + anchorView.getHeight() + 4; // 4dp gap
            
            // Ensure popup doesn't go off screen
            int screenWidth = anchorView.getContext().getResources().getDisplayMetrics().widthPixels;
            int popupWidth = popupWindow.getContentView().getMeasuredWidth();
            
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
}