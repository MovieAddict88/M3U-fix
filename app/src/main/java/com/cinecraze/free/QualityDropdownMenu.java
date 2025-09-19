package com.cinecraze.free;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cinecraze.free.models.Server;

import java.util.List;

public class QualityDropdownMenu {

    private PopupWindow popupWindow;
    private List<Server> servers;
    private OnQualitySelectedListener listener;
    private int currentServerIndex;

    public interface OnQualitySelectedListener {
        void onQualitySelected(Server server, int position);
    }

    public QualityDropdownMenu(Context context, List<Server> servers, int currentServerIndex) {
        this.servers = servers;
        this.currentServerIndex = currentServerIndex;
        createPopupWindow(context);
    }

    private void createPopupWindow(Context context) {
        // Create ListView for quality options
        ListView listView = new ListView(context);
        listView.setBackgroundColor(0xFF2C2C2C); // Dark background
        listView.setDividerHeight(1);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#404040")));
        // Create adapter with custom styling
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, 
                android.R.layout.simple_list_item_1) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                
                if (position < servers.size()) {
                    Server server = servers.get(position);
                    textView.setText(server.getName());
                    textView.setTextColor(0xFFFFFFFF); // White text
                    textView.setPadding(32, 16, 32, 16);
                    
                    // Highlight current selection
                    if (position == currentServerIndex) {
                        textView.setBackgroundColor(0xFF007ACC); // Blue background for selected
                    } else {
                        textView.setBackgroundColor(0x00000000); // Transparent for others
                    }
                }
                
                return view;
            }
        };

        // Add server names to adapter
        for (Server server : servers) {
            adapter.add(server.getName());
        }

        listView.setAdapter(adapter);

        // Handle item clicks
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null && position < servers.size()) {
                listener.onQualitySelected(servers.get(position), position);
            }
            dismiss();
        });

        // Create PopupWindow
        popupWindow = new PopupWindow(listView, ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(context.getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
        popupWindow.setElevation(8f);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
    }

    public void setOnQualitySelectedListener(OnQualitySelectedListener listener) {
        this.listener = listener;
    }

    public void show(View anchorView) {
        if (popupWindow != null && !popupWindow.isShowing()) {
            // Calculate position to show above or below the anchor
            int[] location = new int[2];
            anchorView.getLocationInWindow(location);
            
            int x = location[0];
            int y = location[1] - anchorView.getHeight();
            
            // Show above the anchor view
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