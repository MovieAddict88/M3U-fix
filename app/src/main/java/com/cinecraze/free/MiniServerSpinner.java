package com.cinecraze.free;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.cinecraze.free.models.Server;

import java.util.List;

public class MiniServerSpinner {

    private PopupWindow popupWindow;
    private List<Server> servers;
    private OnServerSelectedListener listener;
    private int currentServerIndex;

    public interface OnServerSelectedListener {
        void onServerSelected(Server server, int position);
    }

    public MiniServerSpinner(Context context, List<Server> servers, int currentServerIndex) {
        this.servers = servers;
        this.currentServerIndex = currentServerIndex;
        createMiniPopupWindow(context);
    }

    private void createMiniPopupWindow(Context context) {
        // Create horizontal LinearLayout for compact server options
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(8, 4, 8, 4);
        
        // Create compact background with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#2C2C2C"));
        background.setCornerRadius(6f);
        background.setStroke(1, Color.parseColor("#404040"));
        container.setBackground(background);
        
        // Add server buttons
        for (int i = 0; i < servers.size(); i++) {
            final int position = i;
            Server server = servers.get(i);
            
            TextView serverButton = new TextView(context);
            serverButton.setText(server.getName());
            serverButton.setTextColor(Color.WHITE);
            serverButton.setTextSize(10); // Very small text
            serverButton.setPadding(8, 4, 8, 4);
            serverButton.setMinWidth(0);
            serverButton.setGravity(Gravity.CENTER);
            
            // Create compact button background
            GradientDrawable buttonBackground = new GradientDrawable();
            if (position == currentServerIndex) {
                buttonBackground.setColor(Color.parseColor("#007ACC"));
            } else {
                buttonBackground.setColor(Color.parseColor("#404040"));
            }
            buttonBackground.setCornerRadius(4f);
            serverButton.setBackground(buttonBackground);
            
            // Add margin between buttons
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (position > 0) {
                params.setMargins(4, 0, 0, 0);
            }
            serverButton.setLayoutParams(params);
            
            // Handle click
            serverButton.setOnClickListener(v -> {
                if (listener != null && position < servers.size()) {
                    listener.onServerSelected(servers.get(position), position);
                }
                dismiss();
            });
            
            container.addView(serverButton);
        }

        // Create compact PopupWindow
        popupWindow = new PopupWindow(container, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
        
        // Set compact background
        GradientDrawable popupBackground = new GradientDrawable();
        popupBackground.setColor(Color.parseColor("#2C2C2C"));
        popupBackground.setCornerRadius(6f);
        popupBackground.setStroke(1, Color.parseColor("#404040"));
        popupWindow.setBackgroundDrawable(popupBackground);
        
        // Add subtle shadow
        popupWindow.setElevation(3f);
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
            int y = location[1] + anchorView.getHeight() + 2; // 2dp gap
            
            // Center horizontally relative to anchor
            int anchorWidth = anchorView.getWidth();
            int popupWidth = popupWindow.getContentView().getMeasuredWidth();
            x = x + (anchorWidth - popupWidth) / 2;
            
            // Ensure popup doesn't go off screen
            int screenWidth = anchorView.getContext().getResources().getDisplayMetrics().widthPixels;
            if (x + popupWidth > screenWidth) {
                x = screenWidth - popupWidth - 8; // 8dp margin from edge
            }
            if (x < 8) {
                x = 8;
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