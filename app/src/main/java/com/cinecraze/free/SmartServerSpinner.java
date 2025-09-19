package com.cinecraze.free;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import com.cinecraze.free.models.Server;

import java.util.List;

public class SmartServerSpinner {

    private CompactServerSpinner compactSpinner;
    private MiniServerSpinner miniSpinner;
    private OnServerSelectedListener listener;
    private boolean useMiniSpinner = false;

    public interface OnServerSelectedListener {
        void onServerSelected(Server server, int position);
    }

    public SmartServerSpinner(Context context, List<Server> servers, int currentServerIndex) {
        // Determine which spinner to use based on screen size and server count
        useMiniSpinner = shouldUseMiniSpinner(context, servers);
        
        if (useMiniSpinner) {
            miniSpinner = new MiniServerSpinner(context, servers, currentServerIndex);
            miniSpinner.setOnServerSelectedListener(new MiniServerSpinner.OnServerSelectedListener() {
                @Override
                public void onServerSelected(Server server, int position) {
                    if (listener != null) {
                        listener.onServerSelected(server, position);
                    }
                }
            });
        } else {
            compactSpinner = new CompactServerSpinner(context, servers, currentServerIndex);
            compactSpinner.setOnServerSelectedListener(new CompactServerSpinner.OnServerSelectedListener() {
                @Override
                public void onServerSelected(Server server, int position) {
                    if (listener != null) {
                        listener.onServerSelected(server, position);
                    }
                }
            });
        }
    }

    private boolean shouldUseMiniSpinner(Context context, List<Server> servers) {
        // Get screen metrics
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        // Use mini spinner if:
        // 1. Screen is small (width < 600dp)
        // 2. Or if there are many servers (> 4) and screen is medium (< 800dp)
        // 3. Or if screen height is limited (< 600dp)
        
        float density = metrics.density;
        int widthDp = (int) (screenWidth / density);
        int heightDp = (int) (screenHeight / density);
        
        boolean isSmallScreen = widthDp < 600 || heightDp < 600;
        boolean hasManyServers = servers.size() > 4;
        boolean isMediumScreen = widthDp < 800;
        
        return isSmallScreen || (hasManyServers && isMediumScreen);
    }

    public void setOnServerSelectedListener(OnServerSelectedListener listener) {
        this.listener = listener;
    }

    public void show(View anchorView) {
        if (useMiniSpinner && miniSpinner != null) {
            miniSpinner.show(anchorView);
        } else if (compactSpinner != null) {
            compactSpinner.show(anchorView);
        }
    }

    public void dismiss() {
        if (useMiniSpinner && miniSpinner != null) {
            miniSpinner.dismiss();
        } else if (compactSpinner != null) {
            compactSpinner.dismiss();
        }
    }

    public boolean isShowing() {
        if (useMiniSpinner && miniSpinner != null) {
            return miniSpinner.isShowing();
        } else if (compactSpinner != null) {
            return compactSpinner.isShowing();
        }
        return false;
    }
}