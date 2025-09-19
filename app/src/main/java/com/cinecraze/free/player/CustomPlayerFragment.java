package com.cinecraze.free.player;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.webkit.WebResourceRequest;
import android.view.Gravity;
import android.widget.Toast;

import com.google.android.exoplayer2.ui.PlayerView;
import com.cinecraze.free.R;
import com.cinecraze.free.utils.VideoServerUtils;
import com.cinecraze.free.EmbedActivity;
import com.cinecraze.free.SmartServerSpinner;
import com.cinecraze.free.models.Server;

import java.util.ArrayList;
import java.util.List;

public class CustomPlayerFragment extends Fragment {

    private static String videoKind;
    private CustomPlayerViewModel mCustomPlayerViewModel;
    private PlayerView mPlayerView;
    // private ImageView ic_media_stop; // Removed - not available in layout
    private RelativeLayout payer_pause_play;
    private View view;
    private Boolean done = false;
    private Boolean isLive = false;
    private TextView text_view_exo_player_live;
    private TextView exo_duration;
    private TextView exo_live;
    private ImageView image_view_exo_player_rotation;
    private Boolean isLandscape = true;
    private ImageView image_view_exo_player_subtitles;
    private RelativeLayout relative_layout_subtitles_dialog;
    private ImageView image_view_dialog_source_close;

    // Subtitle components
    private ArrayList<Subtitle> subtitlesForCast = new ArrayList<>();
    private ArrayList<Subtitle> subtitles = new ArrayList<>();
    private ArrayList<Language> languages = new ArrayList<>();
    private RecyclerView recycler_view_comment_dialog_languages;
    private ProgressBar progress_bar_comment_dialog_subtitles;
    private LanguageAdapter languageAdapter;
    private LinearLayoutManager linearLayoutManagerLanguages;
    private LinearLayoutManager linearLayoutManagerSubtitles;
    private SubtitleAdapter subtitleAdapter;
    private RecyclerView recycler_view_comment_dialog_subtitles;

    private TextView text_view_dialog_subtitles_on_off;
    private RelativeLayout relative_layout_dialog_source_text_color_picker;
    private RelativeLayout relative_layout_dialog_source_background_color_picker;
    private ImageView image_view_dialog_source_plus;
    private ImageView image_view_dialog_source_less;
    private TextView text_view_dialog_source_size_text;
    private Integer selectedLanguage = -1;
    private Integer SetedSelectedLanguage = -1;
    private static Integer videoId;
    private TextView text_view_exo_player_loading_subtitles;
    private ImageView image_view_exo_player_replay_10;
    private ImageView image_view_exo_player_forward_10;
    private ImageView image_view_exo_player_back;

    // Hybrid player components
    private FrameLayout playerContainer;
    private WebView webView;
    private boolean isEmbeddedVideo = false;
    private String currentVideoUrl;
    private String videoTitle;
    private String videoSubTile;
    private String videoImage;

    // In-player server switcher
    private ArrayList<Server> availableServers = new ArrayList<>();
    private int currentServerIndex = 0;
    private View serverSwitchAnchor;

    @Override
    public void onResume() {
        super.onResume();
        if (mCustomPlayerViewModel != null && !isEmbeddedVideo) {
            mCustomPlayerViewModel.play();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCustomPlayerViewModel != null && !isEmbeddedVideo) {
            mCustomPlayerViewModel.pause();
        }
        if (webView != null && isEmbeddedVideo) {
            webView.onPause();
        }
    }

    public static CustomPlayerFragment newInstance(String videoUrl, Boolean isLive, String videoType, String videoTitle, String videoSubTile, String videoImage, Integer videoId_, String _videoKind) {
        CustomPlayerFragment customPlayerFragment = new CustomPlayerFragment();
        Bundle args = new Bundle();
        args.putString("videoUrl", videoUrl);
        args.putString("videoType", videoType);
        args.putString("videoTitle", videoTitle);
        args.putString("videoSubTile", videoSubTile);
        args.putString("videoImage", videoImage);
        args.putInt("videoId", videoId_);
        args.putString("videoKind", _videoKind);
        args.putBoolean("isLive", isLive);
        customPlayerFragment.setArguments(args);
        return customPlayerFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_player, container, false);
        initView();
        return view;
    }

    private void initView() {
        playerContainer = view.findViewById(R.id.player_container);
        mPlayerView = view.findViewById(R.id.video_view);
        // ic_media_stop is not available in the layout, removing this line
        payer_pause_play = view.findViewById(R.id.payer_pause_play);
        text_view_exo_player_live = view.findViewById(R.id.text_view_exo_player_live);
        exo_duration = view.findViewById(R.id.exo_duration);
        exo_live = view.findViewById(R.id.exo_live);
        image_view_exo_player_rotation = view.findViewById(R.id.image_view_exo_player_rotation);
        image_view_exo_player_subtitles = view.findViewById(R.id.image_view_exo_player_subtitles);
        relative_layout_subtitles_dialog = view.findViewById(R.id.relative_layout_subtitles_dialog);
        image_view_dialog_source_close = view.findViewById(R.id.image_view_dialog_source_close);
        text_view_exo_player_loading_subtitles = view.findViewById(R.id.text_view_exo_player_loading_subtitles);
        image_view_exo_player_replay_10 = view.findViewById(R.id.image_view_exo_player_replay_10);
        image_view_exo_player_forward_10 = view.findViewById(R.id.image_view_exo_player_forward_10);
        image_view_exo_player_back = view.findViewById(R.id.image_view_exo_player_back);

        // Get video URL and determine if it's embedded
        Bundle args = getArguments();
        if (args != null) {
            currentVideoUrl = args.getString("videoUrl");
            isLive = args.getBoolean("isLive", false);
            videoTitle = args.getString("videoTitle", "");
            videoSubTile = args.getString("videoSubTile", "");
            videoImage = args.getString("videoImage", "");

            // Optional: receive servers list and index
            currentServerIndex = args.getInt("server_index", 0);
            // Note: Not passing actual server list here; overlay will still show but tap will say no servers

            // Check if this is an embedded video URL
            if (VideoServerUtils.isEmbeddedVideoUrl(currentVideoUrl)) {
                setupEmbeddedVideo();
            } else {
                setupDirectVideo();
            }
        }

        initServerSwitcherOverlay();
        initAction();
    }

    private void initServerSwitcherOverlay() {
        if (playerContainer == null) return;
        TextView anchor = new TextView(getContext());
        anchor.setText("Srv");
        anchor.setTextColor(Color.WHITE);
        anchor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        int padH = dpToPx(6);
        int padV = dpToPx(2);
        anchor.setPadding(padH, padV, padH, padV);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#66000000"));
        bg.setCornerRadius(dpToPx(8));
        bg.setStroke(1, Color.parseColor("#80FFFFFF"));
        anchor.setBackground(bg);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        playerContainer.addView(anchor, lp);
        serverSwitchAnchor = anchor;
        anchor.bringToFront();
        anchor.setElevation(100f);

        anchor.setOnClickListener(v -> {
            if (availableServers == null || availableServers.isEmpty()) {
                Toast.makeText(getContext(), "No other servers", Toast.LENGTH_SHORT).show();
                return;
            }
            SmartServerSpinner spinner = new SmartServerSpinner(getContext(), availableServers, currentServerIndex);
            spinner.setOnServerSelectedListener((server, position) -> switchServer(server, position));
            spinner.show(anchor);
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void switchServer(Server server, int position) {
        if (server == null) return;
        String nextUrl = VideoServerUtils.enhanceVideoUrl(server.getUrl());
        currentServerIndex = position;

        if (VideoServerUtils.isMpdUrl(nextUrl)) {
            // Use EmbedActivity for MPD/DRM
            EmbedActivity.start(getContext(), nextUrl);
            return;
        }

        if (VideoServerUtils.isEmbeddedVideoUrl(nextUrl) || VideoServerUtils.isMultiEmbedUrl(nextUrl) ||
            VideoServerUtils.isYouTubeUrl(nextUrl) || VideoServerUtils.isGoogleDriveUrl(nextUrl) ||
            VideoServerUtils.isMegaUrl(nextUrl)) {
            switchToEmbedded(nextUrl);
            return;
        }

        // Direct stream - reload ExoPlayer in place
        switchToDirect(nextUrl);
    }

    private void switchToEmbedded(String newUrl) {
        isEmbeddedVideo = true;
        currentVideoUrl = newUrl;

        // Hide ExoPlayer and release if needed
        if (mCustomPlayerViewModel != null) {
            mCustomPlayerViewModel.release();
            mCustomPlayerViewModel = null;
        }
        mPlayerView.setVisibility(View.GONE);

        if (webView == null) {
            setupEmbeddedVideo();
        } else {
            webView.loadUrl(newUrl);
        }
    }

    private void switchToDirect(String newUrl) {
        isEmbeddedVideo = false;
        currentVideoUrl = newUrl;

        // Hide WebView if present
        if (webView != null) {
            webView.stopLoading();
            webView.setVisibility(View.GONE);
        }

        // Show ExoPlayer
        mPlayerView.setVisibility(View.VISIBLE);

        // Re-init player
        if (mCustomPlayerViewModel != null) {
            mCustomPlayerViewModel.release();
        }
        mCustomPlayerViewModel = new CustomPlayerViewModel(getActivity());
        Bundle bundle = new Bundle();
        bundle.putString("videoUrl", currentVideoUrl);
        bundle.putBoolean("isLive", isLive);
        bundle.putString("videoType", VideoServerUtils.getVideoType(currentVideoUrl));
        bundle.putString("videoTitle", videoTitle);
        bundle.putString("videoSubTile", videoSubTile);
        bundle.putString("videoImage", videoImage);
        mCustomPlayerViewModel.onStart(mPlayerView, bundle);
    }

    private void setupEmbeddedVideo() {
        isEmbeddedVideo = true;
        Log.d("CustomPlayerFragment", "Setting up embedded video: " + currentVideoUrl);

        // Hide ExoPlayer
        mPlayerView.setVisibility(View.GONE);

        // Create WebView for embedded video
        webView = new WebView(getContext());
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Configure WebView settings for video playback (CinemaX approach)
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(false);
        settings.setSaveFormData(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Additional settings for better video compatibility (from CinemaX)
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);  // Key CinemaX setting - prevents cached ads

        // Enable hardware acceleration for better video performance
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Set WebView client (CinemaX approach)
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("CustomPlayerFragment", "Embedded video page loaded: " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("CustomPlayerFragment", "WebView error: " + description);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Simple CinemaX approach: only allow navigation within the same domain
                if (isWithinVideoServerDomain(url, currentVideoUrl)) {
                    return false; // Allow navigation within video server
                }

                // Block all external navigation (this prevents most ads and redirects)
                Log.d("CustomPlayerFragment", "Blocked external navigation to: " + url);
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                // Continue loading despite SSL errors for video servers (CinemaX approach)
                handler.proceed();
            }
        });

        // Add WebView to container
        playerContainer.removeAllViews();
        playerContainer.addView(webView);

        // Load the embedded URL
        webView.loadUrl(currentVideoUrl);
    }

    private void setupDirectVideo() {
        isEmbeddedVideo = false;
        Log.d("CustomPlayerFragment", "Setting up direct video: " + currentVideoUrl);

        // Show ExoPlayer
        mPlayerView.setVisibility(View.VISIBLE);

        // Initialize ExoPlayer
        mCustomPlayerViewModel = new CustomPlayerViewModel(getActivity());

        // Create bundle with video data
        Bundle bundle = new Bundle();
        bundle.putString("videoUrl", currentVideoUrl);
        bundle.putBoolean("isLive", isLive);
        bundle.putString("videoType", VideoServerUtils.getVideoType(currentVideoUrl));
        bundle.putString("videoTitle", videoTitle);
        bundle.putString("videoSubTile", videoSubTile);
        bundle.putString("videoImage", videoImage);

        mCustomPlayerViewModel.onStart(mPlayerView, bundle);
    }

    private void initAction() {
        // Handle player controls
        if (image_view_exo_player_back != null) {
            image_view_exo_player_back.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (image_view_exo_player_rotation != null) {
            image_view_exo_player_rotation.setOnClickListener(v -> {
                if (getActivity() != null) {
                    if (getActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }
                }
            });
        }

        if (image_view_exo_player_replay_10 != null) {
            image_view_exo_player_replay_10.setOnClickListener(v -> {
                if (!isEmbeddedVideo && mCustomPlayerViewModel != null) {
                    mCustomPlayerViewModel.seekBackward(10000);
                }
            });
        }

        if (image_view_exo_player_forward_10 != null) {
            image_view_exo_player_forward_10.setOnClickListener(v -> {
                if (!isEmbeddedVideo && mCustomPlayerViewModel != null) {
                    mCustomPlayerViewModel.seekForward(10000);
                }
            });
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // The CustomPlayerViewModel is already initialized in setupDirectVideo()
        // No need to call onStart() again here
    }

    public void setFull() {
        if (!isEmbeddedVideo && mCustomPlayerViewModel != null) {
            mCustomPlayerViewModel.setFullscreen(true);
        }
    }

    public void setNormal() {
        if (!isEmbeddedVideo && mCustomPlayerViewModel != null) {
            mCustomPlayerViewModel.setFullscreen(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isEmbeddedVideo && mCustomPlayerViewModel != null) {
            mCustomPlayerViewModel.releasePlayer();
        }
        if (webView != null && isEmbeddedVideo) {
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
    }

    // Language and Subtitle adapters (simplified for this example)
    public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageHolder> {
        @Override
        public LanguageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
            return new LanguageHolder(v);
        }

        @Override
        public void onBindViewHolder(LanguageHolder holder, final int position) {
            // Implementation for language adapter
        }

        @Override
        public int getItemCount() {
            return languages.size();
        }

        public class LanguageHolder extends RecyclerView.ViewHolder {
            public LanguageHolder(View itemView) {
                super(itemView);
            }
        }
    }

    public class SubtitleAdapter extends RecyclerView.Adapter<SubtitleAdapter.SubtitleHolder> {
        @Override
        public SubtitleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subtitle, parent, false);
            return new SubtitleHolder(v);
        }

        @Override
        public void onBindViewHolder(SubtitleHolder holder, final int position) {
            // Implementation for subtitle adapter
        }

        @Override
        public int getItemCount() {
            return subtitles.size();
        }

        public class SubtitleHolder extends RecyclerView.ViewHolder {
            public SubtitleHolder(View itemView) {
                super(itemView);
            }
        }
    }

    // Data classes for subtitles and languages
    public static class Language {
        private String language;
        private int id;
        private boolean selected;
        private List<Subtitle> subtitles;

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public boolean getSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
        public List<Subtitle> getSubtitles() { return subtitles; }
        public void setSubtitles(List<Subtitle> subtitles) { this.subtitles = subtitles; }
    }

    public static class Subtitle {
        private String url;
        private String type;
        private String language;
        private boolean selected;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public Boolean getSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }

    // Helper method for domain validation (CinemaX approach)
    private boolean isWithinVideoServerDomain(String url, String currentVideoUrl) {
        try {
            java.net.URI uri = new java.net.URI(url);
            java.net.URI currentUri = new java.net.URI(currentVideoUrl);
            return uri.getHost().equals(currentUri.getHost());
        } catch (Exception e) {
            return false;
        }
    }
}