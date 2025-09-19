package com.cinecraze.free;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.widget.ProgressBar;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.widget.TextView;
import android.view.Gravity;
import android.widget.FrameLayout.LayoutParams;

import androidx.appcompat.app.AppCompatActivity;

import com.cinecraze.free.utils.VideoServerUtils;
import com.cinecraze.free.models.Server;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class EmbedActivity extends AppCompatActivity {
    private WebView webView;
    private FrameLayout customViewContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private View mCustomView;
    private myWebChromeClient mWebChromeClient;
    private myWebViewClient mWebViewClient;
    private String url;
    private String drmLicense;
    private String drmRobustness;
    private boolean isDrmExplicit = false;
    private boolean isDrmFromIntent = false;
    private static final String TAG = "EmbedActivity";
    private ProgressBar loadingProgressBar;
    private Handler timeoutHandler = new Handler();
    private static final int LOADING_TIMEOUT = 30000; // 30 seconds timeout
    private boolean isSuperEmbed = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;
    // MultiEmbed redirect tracking
    private int multiEmbedRedirectCount = 0;
    private static final int MAX_MULTIEMBED_REDIRECTS = 3;

    // In-player server switcher
    private ArrayList<Server> availableServers = new ArrayList<>();
    private int currentServerIndex = 0;
    private SmartServerSpinner serverSpinner;
    private View serverSwitchAnchor;

    public static void start(Context context, String embedUrl) {
        Intent intent = new Intent(context, EmbedActivity.class);
        intent.putExtra("url", embedUrl);
        context.startActivity(intent);
    }

    public static void start(Context context, String embedUrl, String license) {
        Intent intent = new Intent(context, EmbedActivity.class);
        intent.putExtra("url", embedUrl);
        intent.putExtra("license", license);
        context.startActivity(intent);
    }

    public static void startWithConfig(Context context, String embedUrl, String license, boolean isDrm, String drmRobustness) {
        Intent intent = new Intent(context, EmbedActivity.class);
        intent.putExtra("url", embedUrl);
        intent.putExtra("license", license);
        intent.putExtra("is_drm", isDrm);
        if (drmRobustness != null && !drmRobustness.isEmpty()) {
            intent.putExtra("drm_robustness", drmRobustness);
        }
        context.startActivity(intent);
    }

    // New: start with servers list and index
    public static void startWithServers(Context context, String embedUrl, String license, boolean isDrm, String drmRobustness, ArrayList<Server> servers, int currentIndex) {
        Intent intent = new Intent(context, EmbedActivity.class);
        intent.putExtra("url", embedUrl);
        intent.putExtra("license", license);
        intent.putExtra("is_drm", isDrm);
        if (drmRobustness != null && !drmRobustness.isEmpty()) {
            intent.putExtra("drm_robustness", drmRobustness);
        }
        if (servers != null && !servers.isEmpty()) {
            String serversJson = new Gson().toJson(servers);
            intent.putExtra("servers_json", serversJson);
            intent.putExtra("server_index", currentIndex);
        }
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set landscape orientation like CinemaX
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_embed);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        url = getIntent().getStringExtra("url");
        drmLicense = getIntent().getStringExtra("license");
        isDrmFromIntent = getIntent().getBooleanExtra("is_drm", false);
        isDrmExplicit = getIntent().hasExtra("is_drm");
        drmRobustness = getIntent().getStringExtra("drm_robustness");

        // Parse optional servers list and index
        try {
            String serversJson = getIntent().getStringExtra("servers_json");
            currentServerIndex = getIntent().getIntExtra("server_index", 0);
            if (serversJson != null && !serversJson.isEmpty()) {
                ArrayList<Server> parsed = new Gson().fromJson(serversJson, new TypeToken<ArrayList<Server>>(){}.getType());
                if (parsed != null) {
                    availableServers.clear();
                    availableServers.addAll(parsed);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse servers list: " + e.getMessage());
        }

        // Detect video platform type
        isSuperEmbed = url != null && url.contains("superembed.mov");
        boolean isYouTube = VideoServerUtils.isYouTubeUrl(url);
        boolean isGoogleDrive = VideoServerUtils.isGoogleDriveUrl(url);
        boolean isMega = VideoServerUtils.isMegaUrl(url);
        boolean isMultiEmbed = VideoServerUtils.isMultiEmbedUrl(url);
        boolean isMpd = VideoServerUtils.isMpdUrl(url);

        // Enhanced DRM detection with explicit control
        boolean isDrm;
        if (isDrmExplicit) {
            // Use explicit DRM flag from intent
            isDrm = isDrmFromIntent;
            Log.d(TAG, "Using explicit DRM setting: " + isDrm);
        } else {
            // Fallback to URL analysis and license presence
            isDrm = VideoServerUtils.isDrmProtectedUrl(url) || (drmLicense != null && !drmLicense.trim().isEmpty());
            Log.d(TAG, "Auto-detected DRM: " + isDrm);
        }

        initializeViews();
        setupWebView();
        initServerSwitcherOverlay();
        loadUrl();
    }

    private void initializeViews() {
        customViewContainer = findViewById(R.id.customViewContainer);
        webView = findViewById(R.id.webView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupWebView() {
        mWebViewClient = new myWebViewClient();
        webView.setWebViewClient(mWebViewClient);

        mWebChromeClient = new myWebChromeClient();
        webView.setWebChromeClient(mWebChromeClient);

        // Enhanced WebView settings for better video playback (CinemaX approach)
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

        // Enhanced settings for SuperEmbed compatibility
        if (isSuperEmbed) {
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Modern cache control - prevents cached ads
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
        }

        // Enhanced settings for YouTube compatibility
        if (VideoServerUtils.isYouTubeUrl(url)) {
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Modern cache control - prevents cached ads
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
        }

        // Enhanced settings for Google Drive compatibility
        if (VideoServerUtils.isGoogleDriveUrl(url)) {
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Strict no-cache to keep overlays away
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            // Improve touch handling for video controls
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            // Keep UI minimal by injecting Drive fix early after first load
        }

        // Enhanced settings for Mega compatibility and app redirect prevention
        if (VideoServerUtils.isMegaUrl(url)) {
            settings.setJavaScriptCanOpenWindowsAutomatically(false); // Prevent popups
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Strict no-cache + block network loads re-enable after DOM stabilization
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            // Improve touch handling for video controls and prevent app redirects
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            // Block multiple windows to prevent app store redirects
            settings.setSupportMultipleWindows(false);
        }

        // Enhanced settings for MPD/DASH streams with Shaka Player
        if (VideoServerUtils.isMpdUrl(url)) {
            settings.setJavaScriptEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            // Optimize for video streaming
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            // Enhanced settings for DRM content
            if (VideoServerUtils.isDrmProtectedUrl(url)) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            // Disable zoom controls for better video experience
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            Log.d(TAG, "MPD/DASH WebView settings applied. DRM: " + VideoServerUtils.isDrmProtectedUrl(url));
        }

        // Enhanced settings for MultiEmbed compatibility (SuperEmbed alternative)
        if (VideoServerUtils.isMultiEmbedUrl(url)) {
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Modern cache control - prevents cached ads (like VidSrc and VidJoy)
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            // Additional settings for better compatibility as SuperEmbed alternative
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setAllowContentAccess(true);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            // Enable redirect following
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            // Additional settings for better redirect handling
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
        }

        // Enhanced settings for StreamingNow.mov (actual video source)
        if (VideoServerUtils.isStreamingNowUrl(url)) {
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            // Aggressive cache control for streamingnow.mov to prevent countdown ads
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            // Additional settings for streamingnow.mov
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setAllowContentAccess(true);
            settings.setBuiltInZoomControls(false);
            settings.setDisplayZoomControls(false);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setJavaScriptEnabled(true);
        }

        // Enable hardware acceleration for better video performance
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Set custom user agent for better compatibility
        settings.setUserAgentString(VideoServerUtils.getUserAgent());

        // Preferred embed domains (vidsrc.*, embed.su, vidlink.pro, autoembed)
        boolean isPreferredEmbed = url != null && (
                url.contains("vidsrc.to") || url.contains("vidsrc.net") || url.contains("vidsrc.me") || url.contains("vidsrc.xyz") ||
                url.contains("embed.su") || url.contains("vidlink.pro") ||
                url.contains("player.autoembed.cc") || url.contains("autoembed.cc")
        );
        if (isPreferredEmbed) {
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAllowUniversalAccessFromFileURLs(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setLoadsImagesAutomatically(true);
            settings.setBlockNetworkImage(false);
            settings.setBlockNetworkLoads(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setSupportMultipleWindows(false);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
        }
    }

    private void initServerSwitcherOverlay() {
        if (availableServers == null) {
            return;
        }
        // Create a tiny anchor view (approx 8dp height) on top-right
        TextView anchor = new TextView(this);
        anchor.setText("Srv");
        anchor.setTextColor(android.graphics.Color.WHITE);
        anchor.setTextSize(10);
        int padH = dpToPx(6);
        int padV = dpToPx(2);
        anchor.setPadding(padH, padV, padH, padV);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(android.graphics.Color.parseColor("#66000000"));
        bg.setCornerRadius(dpToPx(8));
        bg.setStroke(1, android.graphics.Color.parseColor("#80FFFFFF"));
        anchor.setBackground(bg);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lp.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        addContentView(anchor, lp);
        serverSwitchAnchor = anchor;
        anchor.bringToFront();
        anchor.setElevation(100f);

        anchor.setOnClickListener(v -> {
            if (availableServers == null || availableServers.isEmpty()) {
                Toast.makeText(EmbedActivity.this, "No other servers", Toast.LENGTH_SHORT).show();
                return;
            }
            serverSpinner = new SmartServerSpinner(EmbedActivity.this, availableServers, currentServerIndex);
            serverSpinner.setOnServerSelectedListener((server, position) -> switchServer(server, position));
            serverSpinner.show(anchor);
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void switchServer(Server server, int position) {
        if (server == null) return;
        try {
            String nextUrl = VideoServerUtils.enhanceVideoUrl(server.getUrl());
            boolean nextIsEmbedded = VideoServerUtils.isEmbeddedVideoUrl(nextUrl) || VideoServerUtils.isMultiEmbedUrl(nextUrl) || VideoServerUtils.isYouTubeUrl(nextUrl) || VideoServerUtils.isGoogleDriveUrl(nextUrl) || VideoServerUtils.isMegaUrl(nextUrl);
            boolean nextIsMpd = VideoServerUtils.isMpdUrl(nextUrl);

            currentServerIndex = position;

            if (nextIsMpd) {
                // Switch within this activity using Shaka Player HTML
                url = nextUrl;
                drmLicense = server.getLicense();
                isDrmExplicit = true;
                isDrmFromIntent = server.isDrmProtected();
                loadShakaPlayerHtml(nextUrl);
                Toast.makeText(this, "Switching to " + server.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (nextIsEmbedded) {
                // Regular embedded page; reload WebView
                url = nextUrl;
                if (loadingProgressBar != null) loadingProgressBar.setVisibility(View.VISIBLE);
                webView.stopLoading();
                webView.loadUrl(nextUrl);
                Toast.makeText(this, "Switching to " + server.getName(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Not embedded => switch to FullScreenActivity
            String serversJson = new Gson().toJson(availableServers);
            FullScreenActivity.startWithServers(this, nextUrl, 0, true, position, serversJson, this.drmRobustness);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error switching server: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to switch server", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUrl() {
        // Validate URL before loading
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Invalid video URL", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Loading URL: " + url);

        // Special handling for MPD/DASH URLs - use Shaka Player
        if (VideoServerUtils.isMpdUrl(url)) {
            Log.d(TAG, "Detected MPD/DASH URL, loading with Shaka Player");

            // Show loading indicator
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            // Load Shaka Player HTML with the MPD URL
            loadShakaPlayerHtml(url);
            return;
        }

        // Special handling for multiembed URLs
        if (VideoServerUtils.isMultiEmbedUrl(url)) {
            Log.d(TAG, "Detected MultiEmbed URL, applying special handling");
            // For multiembed, we'll load the URL directly and handle redirects in WebView
            String enhancedUrl = VideoServerUtils.enhanceVideoUrl(url);
            Log.d(TAG, "Enhanced MultiEmbed URL: " + enhancedUrl);

            // Show loading indicator
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            webView.loadUrl(enhancedUrl);
        } else {
            // Enhance URL with parameters for better compatibility
            String enhancedUrl = VideoServerUtils.enhanceVideoUrl(url);

            // Show loading indicator
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            webView.loadUrl(enhancedUrl);
        }
    }

    public boolean inCustomView() {
        return (mCustomView != null);
    }

    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (inCustomView()) {
            hideCustomView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up timeout handler
        if (timeoutHandler != null) {
            timeoutHandler.removeCallbacksAndMessages(null);
        }

        // Clean up WebView
        if (webView != null) {
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (inCustomView()) {
                hideCustomView();
                return true;
            }

            if ((mCustomView == null) && webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Load Shaka Player HTML for MPD/DASH streams
     */
    private void loadShakaPlayerHtml(String mpdUrl) {
        boolean isDrmProtected;
        if (isDrmExplicit) {
            isDrmProtected = isDrmFromIntent;
        } else {
            isDrmProtected = VideoServerUtils.isDrmProtectedUrl(mpdUrl) || (drmLicense != null && !drmLicense.trim().isEmpty());
        }

        String html = generateShakaPlayerHtml(mpdUrl, isDrmProtected, drmLicense, drmRobustness);
        // Use a proxy for CORS issues, as localhost might not be whitelisted
        webView.loadDataWithBaseURL("https://shaka-player-proxy.appspot.com", html, "text/html", "UTF-8", null);

        String licenseType = isDrmProtected ? "DRM" : (drmLicense != null ? "Auth/Token" : "None");
        Log.d(TAG, "Shaka Player HTML loaded for MPD URL: " + mpdUrl +
              " (License Type: " + licenseType + ", Explicit DRM: " + isDrmExplicit + ")");
    }

    /**
     * Generate HTML with Shaka Player for DASH/MPD playback
     */
    private String generateShakaPlayerHtml(String mpdUrl, boolean isDrmProtected, String license, String robustness) {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset='utf-8'>\n" +
            "    <meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>\n" +
            "    <title>CineCraze Stream - Shaka Player</title>\n" +
            "    <style>\n" +
            "        body, html {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            background: black;\n" +
            "            overflow: hidden;\n" +
            "            width: 100%;\n" +
            "            height: 100%;\n" +
            "        }\n" +
            "        #videoContainer {\n" +
            "            position: relative;\n" +
            "            width: 100%;\n" +
            "            height: 100vh;\n" +
            "            background: black;\n" +
            "        }\n" +
            "        #video {\n" +
            "            width: 100%;\n" +
            "            height: 100%;\n" +
            "            background: black;\n" +
            "        }\n" +
            "        .error-display {\n" +
            "            position: absolute;\n" +
            "            top: 50%;\n" +
            "            left: 50%;\n" +
            "            transform: translate(-50%, -50%);\n" +
            "            color: white;\n" +
            "            font-family: Arial, sans-serif;\n" +
            "            text-align: center;\n" +
            "            display: none;\n" +
            "        }\n" +
            "        .loading {\n" +
            "            position: absolute;\n" +
            "            top: 50%;\n" +
            "            left: 50%;\n" +
            "            transform: translate(-50%, -50%);\n" +
            "            color: white;\n" +
            "            font-family: Arial, sans-serif;\n" +
            "        }\n" +
            "    </style>\n" +
            "    <script src='https://cdnjs.cloudflare.com/ajax/libs/shaka-player/4.7.0/shaka-player.compiled.js'></script>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id='videoContainer'>\n" +
            "        <video id='video' autoplay muted controls></video>\n" +
            "        <div class='loading' id='loading'>Loading video...</div>\n" +
            "        <div class='error-display' id='error'></div>\n" +
            "    </div>\n" +
            "\n" +
            "    <script>\n" +
            "        const video = document.getElementById('video');\n" +
            "        const errorDisplay = document.getElementById('error');\n" +
            "        const loading = document.getElementById('loading');\n" +
            "\n" +
            "        async function initPlayer() {\n" +
            "            try {\n" +
            "                // Check if Shaka Player is supported\n" +
            "                if (!shaka.Player.isBrowserSupported()) {\n" +
            "                    throw new Error('Browser not supported');\n" +
            "                }\n" +
            "\n" +
            "                const player = new shaka.Player(video);\n" +
            "                \n" +
            "                // Configure player for better performance\n" +
            "                player.configure({\n" +
            "                    streaming: {\n" +
            "                        rebufferingGoal: 3,\n" +
            "                        bufferingGoal: 10,\n" +
            "                        bufferBehind: 10\n" +
            "                    },\n" +
            "                    abr: {\n" +
            "                        enabled: true\n" +
            "                    }\n" +
            "                });\n" +
            "\n" +
            (isDrmProtected ?
            "                // Configure DRM with license from playlist\n" +
            "                player.configure({\n" +
            "                    drm: {\n" +
            generateDrmConfiguration(license, robustness) +
            "                    }\n" +
            "                });\n" : ""
            ) +
            "\n" +
            "                // Error handling\n" +
            "                player.addEventListener('error', onError);\n" +
            "\n" +
            generateNonDrmLicenseConfig(license, isDrmProtected) +
            "                // Load the MPD manifest\n" +
            "                await player.load('" + mpdUrl + "');\n" +
            "                \n" +
            "                // Hide loading, show video\n" +
            "                loading.style.display = 'none';\n" +
            "                console.log('Shaka Player loaded successfully');\n" +
            "                \n" +
            "                // Auto-unmute after a short delay (for better UX)\n" +
            "                setTimeout(() => {\n" +
            "                    video.muted = false;\n" +
            "                }, 1000);\n" +
            "\n" +
            "            } catch (error) {\n" +
            "                onError(error);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        function onError(error) {\n" +
            "            console.error('Shaka Player Error:', JSON.stringify(error, null, 2));\n" +
            "            loading.style.display = 'none';\n" +
            "            errorDisplay.style.display = 'block';\n" +
            "            var message = 'Unknown error';\n" +
            "            if (error && error.detail) {\n" +
            "                // Prepend Shaka error code for easier debugging\n" +
            "                message = 'Shaka Error ' + error.detail.code + ': ' + error.detail.message;\n" +
            "            } else if (error && error.message) {\n" +
            "                message = error.message;\n" +
            "            }\n" +
            "            errorDisplay.innerHTML = 'Error loading video: ' + message;\n" +
            "        }\n" +
            "\n" +
            "        // Initialize player when page loads\n" +
            "        document.addEventListener('DOMContentLoaded', initPlayer);\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * Generate DRM configuration based on license format and DRM requirement
     */
    private String generateDrmConfiguration(String license, String robustness) {
        // Check if DRM is explicitly disabled
        if (isDrmExplicit && !isDrmFromIntent) {
            Log.d(TAG, "DRM explicitly disabled, license will be used for authentication/headers");
            return ""; // No DRM configuration
        }

        // Use provided robustness or default to SW_SECURE_CRYPTO
        String widevineRobustness = (robustness != null && !robustness.isEmpty()) ? robustness : "SW_SECURE_CRYPTO";
        Log.d(TAG, "Using Widevine robustness level: " + widevineRobustness);

        if (license != null && !license.trim().isEmpty()) {
            String trimmedLicense = license.trim();

            // 1. Check for license URL (most common case for Widevine/PlayReady)
            if (trimmedLicense.startsWith("http://") || trimmedLicense.startsWith("https://")) {
                Log.d(TAG, "Configuring Widevine/PlayReady DRM with license server: " + trimmedLicense);
                return "                        servers: {\n" +
                       "                            'com.widevine.alpha': '" + trimmedLicense + "',\n" +
                       "                            'com.microsoft.playready': '" + trimmedLicense + "'\n" +
                       "                        },\n" +
                       "                        advanced: {\n" +
                       "                            'com.widevine.alpha': {\n" +
                       "                                'videoRobustness': '" + widevineRobustness + "',\n" +
                       "                                'audioRobustness': '" + widevineRobustness + "'\n" +
                       "                            }\n" +
                       "                        }\n";
            }

            // 2. Check for clearkey format (keyId:key)
            if (trimmedLicense.contains(":") && trimmedLicense.split(":").length == 2 && !trimmedLicense.contains(" ")) {
                String[] parts = trimmedLicense.split(":");
                String keyId = parts[0].trim();
                String key = parts[1].trim();
                // Basic validation for keyId and key (e.g., hex strings)
                if (keyId.matches("^[a-fA-F0-9]+$") && key.matches("^[a-fA-F0-9]+$")) {
                    Log.d(TAG, "Configuring Clearkey DRM with provided keys");
                    return "                        clearKeys: {\n" +
                           "                            '" + keyId + "': '" + key + "'\n" +
                           "                        }\n";
                }
            }

            // 3. Check for JSON format (advanced configurations)
            if (trimmedLicense.startsWith("{") && trimmedLicense.endsWith("}")) {
                Log.d(TAG, "Configuring DRM with provided JSON object");
                // Pass the JSON directly into the configuration
                return trimmedLicense;
            }

            // Fallback: If it's not a URL, clearkey, or JSON, it might be a malformed license.
            // Log a warning but still try to use it as a license server URL as a last resort.
            Log.w(TAG, "Unrecognized license format. Attempting to use as a license server URL: " + trimmedLicense);
            return "                        servers: { 'com.widevine.alpha': '" + trimmedLicense + "' }\n";

        } else {
            // Default configuration for DRM detection when no specific license is provided
            Log.d(TAG, "Using default DRM configuration (no specific license)");
            return "                        servers: {\n" +
                   "                            'com.widevine.alpha': 'https://cwip-shaka-proxy.appspot.com/no_auth',\n" +
                   "                            'com.microsoft.playready': 'https://cwip-shaka-proxy.appspot.com/no_auth'\n" +
                   "                        },\n" +
                   "                        advanced: {\n" +
                   "                            'com.widevine.alpha': {\n" +
                   "                                'videoRobustness': '" + widevineRobustness + "',\n" +
                   "                                'audioRobustness': '" + widevineRobustness + "'\n" +
                   "                            }\n" +
                   "                        }\n";
        }
    }

    /**
     * Generate non-DRM license configuration (for authentication headers, tokens, etc.)
     */
    private String generateNonDrmLicenseConfig(String license, boolean isDrmProtected) {
        // Only add non-DRM license config if we have a license but no DRM
        if (license != null && !license.trim().isEmpty() && !isDrmProtected) {
            Log.d(TAG, "Configuring non-DRM license as authentication token/header");
            return "                // Configure authentication headers for non-DRM license\n" +
                   "                player.getNetworkingEngine().registerRequestFilter((type, request) => {\n" +
                   "                    if (type === shaka.net.NetworkingEngine.RequestType.MANIFEST ||\n" +
                   "                        type === shaka.net.NetworkingEngine.RequestType.SEGMENT) {\n" +
                   "                        // Add license as authorization header\n" +
                   "                        request.headers['Authorization'] = 'Bearer " + license + "';\n" +
                   "                        // Or add as custom header\n" +
                   "                        request.headers['X-Auth-Token'] = '" + license + "';\n" +
                   "                        console.log('Added auth headers for request: ' + request.uris[0]);\n" +
                   "                    }\n" +
                   "                });\n" +
                   "                \n";
        }
        return "";
    }

    class myWebChromeClient extends WebChromeClient {
        private Bitmap mDefaultVideoPoster;
        private View mVideoProgressView;

        @Override
        public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
            onShowCustomView(view, callback);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }
            mCustomView = view;
            webView.setVisibility(View.GONE);
            customViewContainer.setVisibility(View.VISIBLE);
            customViewContainer.addView(view);
            customViewCallback = callback;
        }

        @Override
        public View getVideoLoadingProgressView() {
            if (mVideoProgressView == null) {
                LayoutInflater inflater = LayoutInflater.from(EmbedActivity.this);
                mVideoProgressView = inflater.inflate(R.layout.video_progress, null);
            }
            return mVideoProgressView;
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
            if (mCustomView == null)
                return;

            webView.setVisibility(View.VISIBLE);
            customViewContainer.setVisibility(View.GONE);

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            customViewContainer.removeView(mCustomView);
            customViewCallback.onCustomViewHidden();

            mCustomView = null;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            // Hide loading indicator when progress is complete
            if (newProgress >= 100 && loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.GONE);
            }
        }
    }

    class myWebViewClient extends WebViewClient {
        private int loadAttempts = 0;
        private static final int MAX_ATTEMPTS = 3;

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Navigation attempt to: " + url);

            // Enhanced Mega app blocking - block all possible Mega app redirects
            if (url.contains("mega://") || url.contains("megaapp://") ||
                url.contains("intent://mega.nz") || url.contains("market://") ||
                url.contains("play.google.com/store/apps") ||
                url.contains("/download") || url.contains("/install") ||
                (url.contains("mega.nz") && (url.contains("?download") || url.contains("#download") ||
                 url.contains("action=download") || url.contains("/file/") && url.contains("#")))) {
                Log.d(TAG, "Blocked Mega app/download redirect to: " + url);
                return true; // Block the redirect
            }

            // For Mega URLs, ensure we stay on the web player
            if (VideoServerUtils.isMegaUrl(EmbedActivity.this.url)) {
                // Only allow mega.nz web URLs that don't trigger app downloads
                if (url.contains("mega.nz") && !url.contains("mega://") &&
                    !url.contains("megaapp://") && !url.contains("/download") &&
                    !url.contains("action=download") && !url.contains("?download") &&
                    !url.contains("#download") && !url.contains("intent://")) {
                    Log.d(TAG, "Allowing safe mega.nz web navigation to: " + url);
                    return false; // Allow safe navigation within mega.nz web
                } else {
                    Log.d(TAG, "Blocked unsafe mega.nz navigation to: " + url);
                    return true; // Block unsafe navigation
                }
            }

            // Allow redirects for multiembed to streamingnow.mov
            if (VideoServerUtils.isMultiEmbedUrl(EmbedActivity.this.url) &&
                (url.contains("streamingnow.mov") || url.contains("multiembed.mov"))) {
                Log.d(TAG, "Allowing multiembed redirect to: " + url);
                return false; // Allow the redirect
            }

            // Allow redirects within the same domain chain for MultiEmbed
            if (VideoServerUtils.isMultiEmbedUrl(EmbedActivity.this.url) &&
                (url.contains("multiembed.mov") || url.contains("streamingnow.mov"))) {
                Log.d(TAG, "Allowing multiembed domain redirect to: " + url);
                return false; // Allow the redirect
            }

            // Allow navigation within streamingnow.mov domain
            if (VideoServerUtils.isStreamingNowUrl(EmbedActivity.this.url) &&
                url.contains("streamingnow.mov")) {
                Log.d(TAG, "Allowing streamingnow.mov navigation to: " + url);
                return false; // Allow navigation within streamingnow.mov
            }

            // Simple CinemaX approach: only allow navigation within the same domain
            if (isWithinVideoServerDomain(url, EmbedActivity.this.url)) {
                return false; // Allow navigation within video server
            }

            // Relaxed rules for preferred embed providers
            if (isPreferredEmbedDomain(EmbedActivity.this.url)) {
                if (isPreferredEmbedDomain(url)) {
                    Log.d(TAG, "Allowing preferred embed intra-network redirect: " + url);
                    return false;
                }
                // vidsrc often uses short hops on same registrable domain; allow same eTLD+1
                if (isSameRegistrableDomain(url, EmbedActivity.this.url)) {
                    Log.d(TAG, "Allowing same registrable domain redirect: " + url);
                    return false;
                }
            }

            // Block all external navigation (this prevents most ads and redirects)
            Log.d(TAG, "Blocked external navigation to: " + url);
            return true;
                }

        private boolean isPreferredEmbedDomain(String anyUrl) {
            if (anyUrl == null) return false;
            String u = anyUrl.toLowerCase();
            return u.contains("vidsrc.to") || u.contains("vidsrc.net") || u.contains("vidsrc.me") || u.contains("vidsrc.xyz") ||
                   u.contains("embed.su") || u.contains("vidlink.pro") || u.contains("player.autoembed.cc") || u.contains("autoembed.cc");
        }

        private boolean isSameRegistrableDomain(String urlA, String urlB) {
            try {
                java.net.URI a = new java.net.URI(urlA);
                java.net.URI b = new java.net.URI(urlB);
                String ha = a.getHost();
                String hb = b.getHost();
                if (ha == null || hb == null) return false;
                String ra = getRegistrable(ha);
                String rb = getRegistrable(hb);
                return ra.equals(rb);
            } catch (Exception e) {
                return false;
            }
        }

        private String getRegistrable(String host) {
            // Simple heuristic: take last two labels (e.g., example.com, vidsrc.net)
            String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                return parts[parts.length - 2] + "." + parts[parts.length - 1];
            }
            return host;
        }
 
         @Override
         public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "Page started loading: " + url);

            // Track MultiEmbed redirects
            if (VideoServerUtils.isMultiEmbedUrl(EmbedActivity.this.url) &&
                (url.contains("multiembed.mov") || url.contains("streamingnow.mov"))) {
                multiEmbedRedirectCount++;
                Log.d(TAG, "MultiEmbed redirect count: " + multiEmbedRedirectCount);

                // Prevent infinite redirect loops
                if (multiEmbedRedirectCount > MAX_MULTIEMBED_REDIRECTS) {
                    Log.e(TAG, "Too many MultiEmbed redirects, stopping");
                    Toast.makeText(EmbedActivity.this,
                        "Too many redirects. Please try a different source.",
                        Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }

            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
            }

            // Set timeout for loading - shorter timeout for MultiEmbed to prevent infinite loading
            int timeout = LOADING_TIMEOUT;
            if (VideoServerUtils.isMultiEmbedUrl(EmbedActivity.this.url) ||
                VideoServerUtils.isMultiEmbedRedirectUrl(url)) {
                timeout = 15000; // 15 seconds for MultiEmbed (shorter than default 30s)
                Log.d(TAG, "Using shorter timeout for MultiEmbed: " + timeout + "ms");
            }

            timeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (loadingProgressBar != null && loadingProgressBar.getVisibility() == View.VISIBLE) {
                        Log.w(TAG, "Loading timeout reached for URL: " + url);
                        Toast.makeText(EmbedActivity.this,
                            "Video loading timeout. Please try a different source.",
                            Toast.LENGTH_LONG).show();
                        if (loadingProgressBar != null) {
                            loadingProgressBar.setVisibility(View.GONE);
                        }
                    }
                }
            }, timeout);

            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG, "Page finished loading: " + url);

            // Reset MultiEmbed redirect counter on successful page load
            if (VideoServerUtils.isMultiEmbedUrl(EmbedActivity.this.url) &&
                VideoServerUtils.isMultiEmbedRedirectUrl(url)) {
                Log.d(TAG, "MultiEmbed final page loaded successfully, resetting redirect counter");
                multiEmbedRedirectCount = 0;
            }

                        if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(View.GONE);
            }
 
            // Remove timeout callback
            timeoutHandler.removeCallbacksAndMessages(null);

            // Inject JavaScript for vidsrc/embed.su/vidlink/autoembed
            if (isPreferredEmbedDomain(url)) {
                injectGenericEmbedFix(view);
                // try again after a short delay to catch late DOM changes
                new Handler().postDelayed(() -> injectGenericEmbedFix(view), 1500);
            }
 
            // Inject JavaScript for SuperEmbed to fix black screen issue
            if (isSuperEmbed) {
                injectSuperEmbedFix(view);
            }

            // Inject JavaScript for YouTube to enhance playback
            if (VideoServerUtils.isYouTubeUrl(url)) {
                injectYouTubeFix(view);
            }

            // Inject JavaScript for Google Drive to enhance playback
            if (VideoServerUtils.isGoogleDriveUrl(url)) {
                Log.d(TAG, "Google Drive page finished loading, applying pure video player fix...");
                injectGoogleDriveFix(view);
                new Handler().postDelayed(() -> injectGoogleDriveFix(view), 1000);
                new Handler().postDelayed(() -> injectGoogleDriveFix(view), 3000);
                new Handler().postDelayed(() -> injectGoogleDriveFix(view), 5000);
                // Additional: ensure any drive toolbar is hidden again later
                new Handler().postDelayed(() -> injectGoogleDriveFix(view), 8000);
            }

            // Inject JavaScript for Mega to enhance playback
            if (VideoServerUtils.isMegaUrl(url)) {
                Log.d(TAG, "Mega.nz page finished loading, applying pure video player fix...");
                injectMegaFix(view);
                new Handler().postDelayed(() -> injectMegaFix(view), 1000);
                new Handler().postDelayed(() -> injectMegaFix(view), 3000);
                new Handler().postDelayed(() -> injectMegaFix(view), 5000);
                // Additional: one more pass for late banners
                new Handler().postDelayed(() -> injectMegaFix(view), 8000);
            }

            // Handle MPD/DASH streams - Shaka Player is already loaded in HTML
            if (VideoServerUtils.isMpdUrl(EmbedActivity.this.url)) {
                Log.d(TAG, "MPD/DASH page finished loading, initializing Shaka Player...");
                // Apply additional enhancements for Shaka Player
                injectShakaPlayerEnhancements(view);
            }

            // Inject JavaScript for MultiEmbed to enhance playback
            if (VideoServerUtils.isMultiEmbedUrl(url) || VideoServerUtils.isMultiEmbedRedirectUrl(url)) {
                Log.d(TAG, "MultiEmbed page finished loading, applying fixes...");

                // Check if we're on the final streamingnow.mov page
                if (VideoServerUtils.isMultiEmbedRedirectUrl(url)) {
                    Log.d(TAG, "Detected final MultiEmbed redirect page, applying StreamingNow fix");
                    // Apply specialized fix for streamingnow.mov countdown ads
                    injectStreamingNowFix(view);
                    // Apply additional fix after delay
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            injectStreamingNowFix(view);
                        }
                    }, 2000); // 2 second delay
                } else {
                    // Add delay for multiembed to ensure redirect is complete
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            injectMultiEmbedFix(view);
                            // Apply additional fix after a longer delay to ensure video loads
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    injectMultiEmbedFix(view);
                                }
                            }, 3000); // Additional 3 second delay
                        }
                    }, 2000); // 2 second delay for redirect
                }
            }

            // Inject JavaScript for StreamingNow.mov specifically
            if (VideoServerUtils.isStreamingNowUrl(url)) {
                Log.d(TAG, "StreamingNow.mov page finished loading, applying countdown ad fix...");
                // Apply specialized fix for streamingnow.mov countdown ads
                injectStreamingNowFix(view);
                // Apply additional fix after delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        injectStreamingNowFix(view);
                    }
                }, 2000); // 2 second delay
            }

            super.onPageFinished(view, url);
                }

        /**
         * Generic embed fix: remove overlays/ads, force video/iframe visible, autoplay
         */
        private void injectGenericEmbedFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying generic embed fix...');" +
                "  var selectors = [\"[class*=ad]\",\"[id*=ad]\",\"[class*=ads]\",\"[id*=ads]\",\"[class*=overlay]\",\"[class*=popup]\",\"[class*=modal]\"];" +
                "  selectors.forEach(function(s){document.querySelectorAll(s).forEach(function(e){e.style.display='none';e.style.visibility='hidden';e.style.opacity='0';e.style.pointerEvents='none';});});" +
                "  var videos = document.querySelectorAll('video');" +
                "  videos.forEach(function(v){v.style.display='block';v.style.visibility='visible';v.style.opacity='1';v.style.width='100%';v.style.height='100%';v.controls=true;v.autoplay=true;v.muted=false;v.playsInline=true;});" +
                "  var iframes = document.querySelectorAll('iframe');" +
                "  iframes.forEach(function(f){f.style.display='block';f.style.visibility='visible';f.style.opacity='1';f.style.width='100%';f.style.height='100%';f.style.border='none';});" +
                "  setTimeout(function(){videos.forEach(function(v){if(v.paused){v.play().catch(()=>{});}});}, 800);" +
                "  console.log('Generic embed fix applied');" +
                "} catch(e) { console.log('Generic embed fix error: ' + e.message); }";
            view.evaluateJavascript(jsCode, null);
        }
 
         /**
          * Inject JavaScript to fix SuperEmbed black screen issue and block ads
          */
        private void injectSuperEmbedFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying SuperEmbed fix with enhanced ad blocking...');" +
                "  " +
                "  // Enhanced ad blocking - remove all ad-related elements" +
                "  var adSelectors = ['[class*=\"ad\"]', '[class*=\"ads\"]', '[class*=\"advertisement\"]', '[id*=\"ad\"]', '[id*=\"ads\"]', " +
                "                   '.ad-overlay', '.ad-banner', '.ad-container', '.ad-wrapper', '.advertisement', " +
                "                   '[class*=\"popup\"]', '[class*=\"modal\"]', '[class*=\"overlay\"]', " +
                "                   '.ytp-ad-overlay-container', '.ytp-ad-skip-button', '.ytp-ad-skip-button-modest', " +
                "                   '.ytp-ad-text', '.ytp-ad-text-overlay', '.ytp-ad-feedback-dialog-container'];" +
                "  " +
                "  adSelectors.forEach(function(selector) {" +
                "    var elements = document.querySelectorAll(selector);" +
                "    console.log('Found ' + elements.length + ' ad elements with selector: ' + selector);" +
                "    for(var i = 0; i < elements.length; i++) {" +
                "      elements[i].style.display = 'none';" +
                "      elements[i].style.visibility = 'hidden';" +
                "      elements[i].style.opacity = '0';" +
                "      elements[i].style.pointerEvents = 'none';" +
                "      console.log('Blocked ad element: ' + selector);" +
                "    }" +
                "  });" +
                "  " +
                "  // Force video element to be visible" +
                "  var videos = document.querySelectorAll('video');" +
                "  console.log('Found ' + videos.length + ' video elements');" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    videos[i].style.display = 'block';" +
                "    videos[i].style.visibility = 'visible';" +
                "    videos[i].style.opacity = '1';" +
                "    videos[i].style.zIndex = '9999';" +
                "    videos[i].style.position = 'relative';" +
                "    videos[i].style.width = '100%';" +
                "    videos[i].style.height = '100%';" +
                "    videos[i].style.backgroundColor = 'black';" +
                "    console.log('Fixed video element ' + i);" +
                "  }" +
                "  " +
                "  // Force iframe elements to be visible" +
                "  var iframes = document.querySelectorAll('iframe');" +
                "  console.log('Found ' + iframes.length + ' iframe elements');" +
                "  for(var i = 0; i < iframes.length; i++) {" +
                "    iframes[i].style.display = 'block';" +
                "    iframes[i].style.visibility = 'visible';" +
                "    iframes[i].style.opacity = '1';" +
                "    iframes[i].style.zIndex = '9999';" +
                "    iframes[i].style.position = 'relative';" +
                "    iframes[i].style.width = '100%';" +
                "    iframes[i].style.height = '100%';" +
                "    iframes[i].style.backgroundColor = 'black';" +
                "    console.log('Fixed iframe element ' + i);" +
                "  }" +
                "  " +
                "  // Remove any overlay elements that might be blocking the video" +
                "  var overlays = document.querySelectorAll('.overlay, .ad-overlay, .blocker, .ads, .advertisement');" +
                "  console.log('Found ' + overlays.length + ' overlay elements');" +
                "  for(var i = 0; i < overlays.length; i++) {" +
                "    overlays[i].style.display = 'none';" +
                "    overlays[i].style.visibility = 'hidden';" +
                "    overlays[i].style.opacity = '0';" +
                "    overlays[i].style.pointerEvents = 'none';" +
                "    console.log('Removed overlay element ' + i);" +
                "  }" +
                "  " +
                "  // Force body background to be transparent" +
                "  document.body.style.backgroundColor = 'transparent';" +
                "  document.body.style.background = 'transparent';" +
                "  " +
                "  // Force html background to be transparent" +
                "  document.documentElement.style.backgroundColor = 'transparent';" +
                "  document.documentElement.style.background = 'transparent';" +
                "  " +
                "  // Remove any background colors from containers" +
                "  var containers = document.querySelectorAll('div, section, article');" +
                "  for(var i = 0; i < containers.length; i++) {" +
                "    if(containers[i].style.backgroundColor && containers[i].style.backgroundColor !== 'transparent') {" +
                "      containers[i].style.backgroundColor = 'transparent';" +
                "    }" +
                "  }" +
                "  " +
                "  // Force autoplay on video elements" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    videos[i].autoplay = true;" +
                "    videos[i].muted = false;" +
                "    videos[i].controls = true;" +
                "    videos[i].loop = false;" +
                "    videos[i].preload = 'auto';" +
                "    console.log('Set autoplay for video element ' + i);" +
                "  }" +
                "  " +
                "  // Disable any ad-related scripts" +
                "  var scripts = document.querySelectorAll('script');" +
                "  for(var i = 0; i < scripts.length; i++) {" +
                "    if(scripts[i].src && (scripts[i].src.includes('ad') || scripts[i].src.includes('ads') || scripts[i].src.includes('analytics'))) {" +
                "      scripts[i].remove();" +
                "      console.log('Removed ad script: ' + scripts[i].src);" +
                "    }" +
                "  }" +
                "  " +
                "  console.log('SuperEmbed fix with enhanced ad blocking applied successfully');" +
                "} catch(e) {" +
                "  console.log('SuperEmbed fix error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "SuperEmbed fix with enhanced ad blocking applied: " + value);
                }
            });

            // Apply the fix again after a short delay to ensure it takes effect
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.evaluateJavascript(jsCode, null);
                }
            }, 1000);
        }

        /**
         * Inject JavaScript to enhance YouTube playback
         */
        private void injectYouTubeFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying YouTube fix...');" +
                "  " +
                "  // Force YouTube player to be visible and fullscreen" +
                "  var player = document.querySelector('#player');" +
                "  if(player) {" +
                "    player.style.display = 'block';" +
                "    player.style.visibility = 'visible';" +
                "    player.style.opacity = '1';" +
                "    player.style.zIndex = '9999';" +
                "    player.style.position = 'relative';" +
                "    player.style.width = '100%';" +
                "    player.style.height = '100%';" +
                "    console.log('Fixed YouTube player');" +
                "  }" +
                "  " +
                "  // Force video element to be visible" +
                "  var videos = document.querySelectorAll('video');" +
                "  console.log('Found ' + videos.length + ' video elements');" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    videos[i].style.display = 'block';" +
                "    videos[i].style.visibility = 'visible';" +
                "    videos[i].style.opacity = '1';" +
                "    videos[i].style.zIndex = '9999';" +
                "    videos[i].style.position = 'relative';" +
                "    videos[i].style.width = '100%';" +
                "    videos[i].style.height = '100%';" +
                "    videos[i].style.backgroundColor = 'black';" +
                "    videos[i].autoplay = true;" +
                "    videos[i].muted = false;" +
                "    videos[i].controls = true;" +
                "    videos[i].loop = false;" +
                "    videos[i].preload = 'auto';" +
                "    console.log('Fixed video element ' + i);" +
                "  }" +
                "  " +
                "  // Remove YouTube overlays and ads" +
                "  var overlays = document.querySelectorAll('.ytp-pause-overlay, .ytp-gradient-top, .ytp-gradient-bottom, .ytp-show-cards-title, .ytp-watermark');" +
                "  console.log('Found ' + overlays.length + ' YouTube overlay elements');" +
                "  for(var i = 0; i < overlays.length; i++) {" +
                "    overlays[i].style.display = 'none';" +
                "    console.log('Removed YouTube overlay element ' + i);" +
                "  }" +
                "  " +
                "  // Force body background to be transparent" +
                "  document.body.style.backgroundColor = 'transparent';" +
                "  document.body.style.background = 'transparent';" +
                "  " +
                "  console.log('YouTube fix applied successfully');" +
                "} catch(e) {" +
                "  console.log('YouTube fix error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "YouTube fix applied: " + value);
                }
            });
        }

        /**
         * Inject JavaScript to optimize Google Drive video player experience
         */
        private void injectGoogleDriveFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying improved Google Drive video player fix...');" +
                "  " +
                "  // Strategy: Focus on hiding problematic UI elements while preserving touch functionality" +
                "  " +
                "  // Hide Google Drive UI elements that can interfere with video playback" +
                "  var interfaceSelectors = [" +
                "    '[data-target=\"drive.web.downloadDialog\"]'," +
                "    '[data-target=\"drive.web.shareDialog\"]'," +
                "    '[data-tooltip*=\"Download\"]'," +
                "    '[data-tooltip*=\"Share\"]'," +
                "    '[aria-label*=\"Download\"]'," +
                "    '[aria-label*=\"Share\"]'," +
                "    '.gb_fa', '.gb_ea', '.gb_da'," + // Google bar elements
                "    '[jsaction*=\"drive.web\"]'," +
                "    '.ndfHFb-c4YZDc-Wrql6b'," + // Drive interface containers
                "    '.a-s-fa-Ha-pa'," + // Drive action buttons
                "    '.a-s-tb-Ha-pa'," + // Drive toolbar
                "    '.Lvn8L'," + // Drive header
                "    '.gb_uc'," + // Google header
                "    '.gb_Sc'," + // Google notifications
                "    '.drive-viewer-pageless-unloaded'," +
                "    '[role=\"banner\"]'," +
                "    '[role=\"navigation\"]'" +
                "  ];" +
                "  " +
                "  interfaceSelectors.forEach(function(selector) {" +
                "    var elements = document.querySelectorAll(selector);" +
                "    for(var i = 0; i < elements.length; i++) {" +
                "      elements[i].style.display = 'none !important';" +
                "      elements[i].style.visibility = 'hidden !important';" +
                "      elements[i].style.opacity = '0 !important';" +
                "      elements[i].style.pointerEvents = 'none !important';" +
                "      elements[i].style.zIndex = '-9999 !important';" +
                "    }" +
                "  });" +
                "  " +
                "  // Enhance video player elements" +
                "  var videos = document.querySelectorAll('video');" +
                "  console.log('Found ' + videos.length + ' video elements');" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    var video = videos[i];" +
                "    video.style.display = 'block';" +
                "    video.style.visibility = 'visible';" +
                "    video.style.opacity = '1';" +
                "    video.style.width = '100%';" +
                "    video.style.height = '100%';" +
                "    video.style.objectFit = 'contain';" +
                "    video.style.backgroundColor = 'black';" +
                "    video.controls = true;" +
                "    video.preload = 'auto';" +
                "    video.playsInline = true;" +
                "    video.webkitPlaysinline = true;" +
                "    " +
                "    // Ensure video container is properly sized" +
                "    var parent = video.parentElement;" +
                "    if(parent) {" +
                "      parent.style.width = '100%';" +
                "      parent.style.height = '100%';" +
                "      parent.style.position = 'relative';" +
                "      parent.style.backgroundColor = 'black';" +
                "    }" +
                "    console.log('Enhanced video element ' + i);" +
                "  }" +
                "  " +
                "  // Handle iframes" +
                "  var iframes = document.querySelectorAll('iframe[src*=\"drive.google.com\"]');" +
                "  for(var i = 0; i < iframes.length; i++) {" +
                "    var iframe = iframes[i];" +
                "    iframe.style.width = '100%';" +
                "    iframe.style.height = '100%';" +
                "    iframe.style.border = 'none';" +
                "    iframe.style.backgroundColor = 'black';" +
                "  }" +
                "  " +
                "  // Optimize body styling for video playback" +
                "  document.body.style.backgroundColor = 'black';" +
                "  document.body.style.margin = '0';" +
                "  document.body.style.padding = '0';" +
                "  document.body.style.overflow = 'hidden';" +
                "  " +
                "  document.documentElement.style.backgroundColor = 'black';" +
                "  document.documentElement.style.margin = '0';" +
                "  document.documentElement.style.padding = '0';" +
                "  document.documentElement.style.overflow = 'hidden';" +
                "  " +
                "  // Add custom CSS to override Drive styles" +
                "  var style = document.createElement('style');" +
                "  style.textContent = `" +
                "    .drive-viewer-pageless-unloaded { display: none !important; }" +
                "    [data-target=\"drive.web.downloadDialog\"] { display: none !important; }" +
                "    [data-target=\"drive.web.shareDialog\"] { display: none !important; }" +
                "    .gb_fa, .gb_ea, .gb_da { display: none !important; }" +
                "    [role=\"banner\"], [role=\"navigation\"] { display: none !important; }" +
                "    video { touch-action: manipulation !important; }" +
                "    .video-container { touch-action: manipulation !important; }" +
                "  `;" +
                "  document.head.appendChild(style);" +
                "  " +
                "  // Delayed cleanup for dynamically loaded elements" +
                "  setTimeout(function() {" +
                "    interfaceSelectors.forEach(function(selector) {" +
                "      var elements = document.querySelectorAll(selector);" +
                "      for(var i = 0; i < elements.length; i++) {" +
                "        elements[i].style.display = 'none !important';" +
                "      }" +
                "    });" +
                "  }, 1000);" +
                "  " +
                "  console.log('Improved Google Drive video player fix applied successfully');" +
                "} catch(e) {" +
                "  console.log('Google Drive fix error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "Improved Google Drive video player fix applied: " + value);
                }
            });
        }

        /**
         * Inject JavaScript to optimize Mega.nz video player and prevent app redirects
         */
        private void injectMegaFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying improved Mega.nz video player fix...');" +
                "  " +
                "  // Strategy: Hide problematic UI elements while preserving video functionality" +
                "  " +
                "  // Block all download/app redirect elements and scripts" +
                "  var downloadSelectors = [" +
                "    '[class*=\"download\"]'," +
                "    '[id*=\"download\"]'," +
                "    '[data-action=\"download\"]'," +
                "    '[href*=\"/download\"]'," +
                "    '[href*=\"mega://\"]'," +
                "    '[href*=\"megaapp://\"]'," +
                "    '.download-button'," +
                "    '.app-button'," +
                "    '.mobile-app'," +
                "    '.install-app'," +
                "    '.get-app'," +
                "    '[class*=\"banner\"]'," +
                "    '[class*=\"notification\"]'," +
                "    '[class*=\"popup\"]'," +
                "    '[class*=\"modal\"]'," +
                "    '[class*=\"overlay\"]'," +
                "    '.top-menu'," +
                "    '.header'," +
                "    '.footer'," +
                "    '.sidebar'," +
                "    '.ads'," +
                "    '[id*=\"ads\"]'" +
                "  ];" +
                "  " +
                "  downloadSelectors.forEach(function(selector) {" +
                "    var elements = document.querySelectorAll(selector);" +
                "    for(var i = 0; i < elements.length; i++) {" +
                "      elements[i].style.display = 'none !important';" +
                "      elements[i].style.visibility = 'hidden !important';" +
                "      elements[i].style.opacity = '0 !important';" +
                "      elements[i].style.pointerEvents = 'none !important';" +
                "      elements[i].style.zIndex = '-9999 !important';" +
                "      // Remove click handlers" +
                "      elements[i].onclick = null;" +
                "      elements[i].removeAttribute('onclick');" +
                "      elements[i].removeAttribute('href');" +
                "    }" +
                "  });" +
                "  " +
                "  // Remove any links that might redirect to app stores or downloads" +
                "  var links = document.querySelectorAll('a');" +
                "  for(var i = 0; i < links.length; i++) {" +
                "    var href = links[i].href || '';" +
                "    if(href.includes('download') || href.includes('mega://') || " +
                "       href.includes('megaapp://') || href.includes('play.google.com') || " +
                "       href.includes('app.store') || href.includes('market://')) {" +
                "      links[i].style.display = 'none !important';" +
                "      links[i].onclick = function(e) { e.preventDefault(); e.stopPropagation(); return false; };" +
                "      links[i].removeAttribute('href');" +
                "    }" +
                "  }" +
                "  " +
                "  // Enhance video player elements" +
                "  var videos = document.querySelectorAll('video');" +
                "  console.log('Found ' + videos.length + ' video elements');" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    var video = videos[i];" +
                "    video.style.display = 'block';" +
                "    video.style.visibility = 'visible';" +
                "    video.style.opacity = '1';" +
                "    video.style.width = '100%';" +
                "    video.style.height = '100%';" +
                "    video.style.objectFit = 'contain';" +
                "    video.style.backgroundColor = 'black';" +
                "    video.controls = true;" +
                "    video.preload = 'auto';" +
                "    video.playsInline = true;" +
                "    video.webkitPlaysinline = true;" +
                "    " +
                "    // Ensure video container is properly sized" +
                "    var parent = video.parentElement;" +
                "    if(parent) {" +
                "      parent.style.width = '100%';" +
                "      parent.style.height = '100%';" +
                "      parent.style.position = 'relative';" +
                "      parent.style.backgroundColor = 'black';" +
                "    }" +
                "    console.log('Enhanced video element ' + i);" +
                "  }" +
                "  " +
                "  // Handle iframes" +
                "  var iframes = document.querySelectorAll('iframe[src*=\"mega.nz\"]');" +
                "  for(var i = 0; i < iframes.length; i++) {" +
                "    var iframe = iframes[i];" +
                "    iframe.style.width = '100%';" +
                "    iframe.style.height = '100%';" +
                "    iframe.style.border = 'none';" +
                "    iframe.style.backgroundColor = 'black';" +
                "  }" +
                "  " +
                "  // Optimize body styling for video playback" +
                "  document.body.style.backgroundColor = 'black';" +
                "  document.body.style.margin = '0';" +
                "  document.body.style.padding = '0';" +
                "  document.body.style.overflow = 'hidden';" +
                "  " +
                "  document.documentElement.style.backgroundColor = 'black';" +
                "  document.documentElement.style.margin = '0';" +
                "  document.documentElement.style.padding = '0';" +
                "  document.documentElement.style.overflow = 'hidden';" +
                "  " +
                "  // Add custom CSS to override Mega styles and block downloads" +
                "  var style = document.createElement('style');" +
                "  style.textContent = `" +
                "    [class*=\"download\"], [id*=\"download\"] { display: none !important; }" +
                "    [href*=\"mega://\"], [href*=\"megaapp://\"] { display: none !important; }" +
                "    .download-button, .app-button, .mobile-app { display: none !important; }" +
                "    .top-menu, .header, .footer, .sidebar { display: none !important; }" +
                "    [class*=\"banner\"], [class*=\"notification\"] { display: none !important; }" +
                "    [class*=\"popup\"], [class*=\"modal\"], [class*=\"overlay\"] { display: none !important; }" +
                "    video { touch-action: manipulation !important; }" +
                "    .video-container { touch-action: manipulation !important; }" +
                "    body { user-select: none !important; }" +
                "  `;" +
                "  document.head.appendChild(style);" +
                "  " +
                "  // Prevent app redirect scripts from running" +
                "  if(window.location && window.location.href) {" +
                "    Object.defineProperty(window.location, 'href', {" +
                "      set: function(url) {" +
                "        if(url.includes('mega://') || url.includes('megaapp://') || " +
                "           url.includes('download') || url.includes('play.google.com')) {" +
                "          console.log('Blocked app redirect to: ' + url);" +
                "          return;" +
                "        }" +
                "        // Allow other URL changes" +
                "        window.location.assign(url);" +
                "      }" +
                "    });" +
                "  }" +
                "  " +
                "  // Override window.open to prevent app store redirects" +
                "  var originalOpen = window.open;" +
                "  window.open = function(url, target, features) {" +
                "    if(url && (url.includes('mega://') || url.includes('megaapp://') || " +
                "               url.includes('play.google.com') || url.includes('app.store'))) {" +
                "      console.log('Blocked popup redirect to: ' + url);" +
                "      return null;" +
                "    }" +
                "    return originalOpen.call(this, url, target, features);" +
                "  };" +
                "  " +
                "  // Delayed cleanup for dynamically loaded elements" +
                "  setTimeout(function() {" +
                "    downloadSelectors.forEach(function(selector) {" +
                "      var elements = document.querySelectorAll(selector);" +
                "      for(var i = 0; i < elements.length; i++) {" +
                "        elements[i].style.display = 'none !important';" +
                "      }" +
                "    });" +
                "  }, 1000);" +
                "  " +
                "  console.log('Improved Mega.nz video player fix applied successfully');" +
                "} catch(e) {" +
                "  console.log('Mega.nz fix error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "Improved Mega.nz video player fix applied: " + value);
                }
            });
        }

        /**
         * Inject additional enhancements for Shaka Player
         */
        private void injectShakaPlayerEnhancements(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying Shaka Player enhancements...');" +
                "  " +
                "  // Ensure fullscreen experience" +
                "  document.body.style.margin = '0';" +
                "  document.body.style.padding = '0';" +
                "  document.body.style.overflow = 'hidden';" +
                "  document.body.style.backgroundColor = 'black';" +
                "  " +
                "  // Enhance video element" +
                "  var video = document.getElementById('video');" +
                "  if(video) {" +
                "    video.style.width = '100%';" +
                "    video.style.height = '100%';" +
                "    video.style.objectFit = 'contain';" +
                "    video.controls = true;" +
                "    video.playsInline = true;" +
                "    video.webkitPlaysinline = true;" +
                "  }" +
                "  " +
                "  // Add touch-friendly controls" +
                "  var container = document.getElementById('videoContainer');" +
                "  if(container) {" +
                "    container.style.touchAction = 'manipulation';" +
                "  }" +
                "  " +
                "  console.log('Shaka Player enhancements applied successfully');" +
                "} catch(e) {" +
                "  console.log('Shaka Player enhancement error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "Shaka Player enhancements applied: " + value);
                }
            });
        }

        /**
         * Inject JavaScript to enhance MultiEmbed playback and fix black screen
         */
        private void injectMultiEmbedFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying MultiEmbed black screen fix...');" +
                "  " +
                "  // Enhanced ad blocking - remove all ad-related elements" +
                "  var adSelectors = ['[class*=\"ad\"]', '[class*=\"ads\"]', '[class*=\"advertisement\"]', '[id*=\"ad\"]', '[id*=\"ads\"]', " +
                "                   '.ad-overlay', '.ad-banner', '.ad-container', '.ad-wrapper', '.advertisement', " +
                "                   '[class*=\"popup\"]', '[class*=\"modal\"]', '[class*=\"overlay\"]', " +
                "                   '.ytp-ad-overlay-container', '.ytp-ad-skip-button', '.ytp-ad-skip-button-modest', " +
                "                   '.ytp-ad-text', '.ytp-ad-text-overlay', '.ytp-ad-feedback-dialog-container'];" +
                "  " +
                "  adSelectors.forEach(function(selector) {" +
                "    var elements = document.querySelectorAll(selector);" +
                "    console.log('Found ' + elements.length + ' ad elements with selector: ' + selector);" +
                "    for(var i = 0; i < elements.length; i++) {" +
                "      elements[i].style.display = 'none';" +
                "      elements[i].style.visibility = 'hidden';" +
                "      elements[i].style.opacity = '0';" +
                "      elements[i].style.pointerEvents = 'none';" +
                "      console.log('Blocked ad element: ' + selector);" +
                "    }" +
                "  });" +
                "  " +
                "  // Force video element to be visible and fix black screen" +
                "  var videos = document.querySelectorAll('video');" +
                "  console.log('Found ' + videos.length + ' video elements');" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    videos[i].style.display = 'block';" +
                "    videos[i].style.visibility = 'visible';" +
                "    videos[i].style.opacity = '1';" +
                "    videos[i].style.zIndex = '9999';" +
                "    videos[i].style.position = 'relative';" +
                "    videos[i].style.width = '100%';" +
                "    videos[i].style.height = '100%';" +
                "    videos[i].style.backgroundColor = 'black';" +
                "    videos[i].autoplay = true;" +
                "    videos[i].muted = false;" +
                "    videos[i].controls = true;" +
                "    videos[i].loop = false;" +
                "    videos[i].preload = 'auto';" +
                "    videos[i].playsInline = true;" +
                "    videos[i].webkitPlaysinline = true;" +
                "    console.log('Fixed video element ' + i);" +
                "  }" +
                "  " +
                "  // Force iframe elements to be visible and fix black screen" +
                "  var iframes = document.querySelectorAll('iframe');" +
                "  console.log('Found ' + iframes.length + ' iframe elements');" +
                "  for(var i = 0; i < iframes.length; i++) {" +
                "    iframes[i].style.display = 'block';" +
                "    iframes[i].style.visibility = 'visible';" +
                "    iframes[i].style.opacity = '1';" +
                "    iframes[i].style.zIndex = '9999';" +
                "    iframes[i].style.position = 'relative';" +
                "    iframes[i].style.width = '100%';" +
                "    iframes[i].style.height = '100%';" +
                "    iframes[i].style.backgroundColor = 'black';" +
                "    iframes[i].style.border = 'none';" +
                "    iframes[i].style.margin = '0';" +
                "    iframes[i].style.padding = '0';" +
                "    console.log('Fixed iframe element ' + i);" +
                "  }" +
                "  " +
                "  // Force any player containers to be visible" +
                "  var playerContainers = document.querySelectorAll('[class*=\"player\"], [id*=\"player\"], [class*=\"video\"], [id*=\"video\"]');" +
                "  console.log('Found ' + playerContainers.length + ' player containers');" +
                "  for(var i = 0; i < playerContainers.length; i++) {" +
                "    playerContainers[i].style.display = 'block';" +
                "    playerContainers[i].style.visibility = 'visible';" +
                "    playerContainers[i].style.opacity = '1';" +
                "    playerContainers[i].style.zIndex = '9999';" +
                "    playerContainers[i].style.position = 'relative';" +
                "    playerContainers[i].style.width = '100%';" +
                "    playerContainers[i].style.height = '100%';" +
                "    playerContainers[i].style.backgroundColor = 'black';" +
                "    console.log('Fixed player container ' + i);" +
                "  }" +
                "  " +
                "  // Remove any overlay elements that might be blocking the video" +
                "  var overlays = document.querySelectorAll('.overlay, .ad-overlay, .blocker, .ads, .advertisement, [class*=\"overlay\"], [class*=\"blocker\"]');" +
                "  console.log('Found ' + overlays.length + ' overlay elements');" +
                "  for(var i = 0; i < overlays.length; i++) {" +
                "    overlays[i].style.display = 'none';" +
                "    overlays[i].style.visibility = 'hidden';" +
                "    overlays[i].style.opacity = '0';" +
                "    overlays[i].style.pointerEvents = 'none';" +
                "    console.log('Removed overlay element ' + i);" +
                "  }" +
                "  " +
                "  // Force body background to be transparent" +
                "  document.body.style.backgroundColor = 'transparent';" +
                "  document.body.style.background = 'transparent';" +
                "  " +
                "  // Force html background to be transparent" +
                "  document.documentElement.style.backgroundColor = 'transparent';" +
                "  document.documentElement.style.background = 'transparent';" +
                "  " +
                "  // Remove any background colors from containers that might cause black screen" +
                "  var containers = document.querySelectorAll('div, section, article, main');" +
                "  for(var i = 0; i < containers.length; i++) {" +
                "    if(containers[i].style.backgroundColor && containers[i].style.backgroundColor !== 'transparent') {" +
                "      containers[i].style.backgroundColor = 'transparent';" +
                "    }" +
                "    if(containers[i].style.background && containers[i].style.background !== 'transparent') {" +
                "      containers[i].style.background = 'transparent';" +
                "    }" +
                "  }" +
                "  " +
                "  // Force any embed or object elements to be visible" +
                "  var embeds = document.querySelectorAll('embed, object');" +
                "  console.log('Found ' + embeds.length + ' embed/object elements');" +
                "  for(var i = 0; i < embeds.length; i++) {" +
                "    embeds[i].style.display = 'block';" +
                "    embeds[i].style.visibility = 'visible';" +
                "    embeds[i].style.opacity = '1';" +
                "    embeds[i].style.zIndex = '9999';" +
                "    embeds[i].style.position = 'relative';" +
                "    embeds[i].style.width = '100%';" +
                "    embeds[i].style.height = '100%';" +
                "    embeds[i].style.backgroundColor = 'black';" +
                "    console.log('Fixed embed/object element ' + i);" +
                "  }" +
                "  " +
                "  // Disable any ad-related scripts" +
                "  var scripts = document.querySelectorAll('script');" +
                "  for(var i = 0; i < scripts.length; i++) {" +
                "    if(scripts[i].src && (scripts[i].src.includes('ad') || scripts[i].src.includes('ads') || scripts[i].src.includes('analytics'))) {" +
                "      scripts[i].remove();" +
                "      console.log('Removed ad script: ' + scripts[i].src);" +
                "    }" +
                "  }" +
                "  " +
                "  // Additional fix for streamingnow.mov redirect" +
                "  if(window.location.hostname.includes('streamingnow.mov')) {" +
                "    console.log('Detected streamingnow.mov redirect, applying additional fixes...');" +
                "    " +
                "    // Force any video containers to be visible" +
                "    var videoContainers = document.querySelectorAll('[class*=\"stream\"], [id*=\"stream\"], [class*=\"embed\"], [id*=\"embed\"]');" +
                "    for(var i = 0; i < videoContainers.length; i++) {" +
                "      videoContainers[i].style.display = 'block';" +
                "      videoContainers[i].style.visibility = 'visible';" +
                "      videoContainers[i].style.opacity = '1';" +
                "      videoContainers[i].style.zIndex = '9999';" +
                "      videoContainers[i].style.position = 'relative';" +
                "      videoContainers[i].style.width = '100%';" +
                "      videoContainers[i].style.height = '100%';" +
                "      videoContainers[i].style.backgroundColor = 'black';" +
                "      console.log('Fixed streaming container ' + i);" +
                "    }" +
                "    " +
                "    // Look for any elements with 'play' in their attributes" +
                "    var playElements = document.querySelectorAll('[src*=\"play\"], [href*=\"play\"], [data-src*=\"play\"]');" +
                "    for(var i = 0; i < playElements.length; i++) {" +
                "      playElements[i].style.display = 'block';" +
                "      playElements[i].style.visibility = 'visible';" +
                "      playElements[i].style.opacity = '1';" +
                "      playElements[i].style.zIndex = '9999';" +
                "      console.log('Fixed play element ' + i);" +
                "    }" +
                "  }" +
                "  " +
                "  // Force autoplay for any video elements" +
                "  setTimeout(function() {" +
                "    var videos = document.querySelectorAll('video');" +
                "    for(var i = 0; i < videos.length; i++) {" +
                "      if(videos[i].paused) {" +
                "        videos[i].play().catch(function(e) {" +
                "          console.log('Autoplay failed for video ' + i + ': ' + e.message);" +
                "        });" +
                "      }" +
                "    }" +
                "  }, 1000);" +
                "  " +
                "  // Additional aggressive fix - look for any hidden elements that might be video players" +
                "  var allElements = document.querySelectorAll('*');" +
                "  for(var i = 0; i < allElements.length; i++) {" +
                "    var element = allElements[i];" +
                "    if(element.style.display === 'none' || element.style.visibility === 'hidden') {" +
                "      if(element.tagName === 'VIDEO' || element.tagName === 'IFRAME' || " +
                "         element.className.includes('player') || element.id.includes('player') || " +
                "         element.className.includes('video') || element.id.includes('video')) {" +
                "        element.style.display = 'block';" +
                "        element.style.visibility = 'visible';" +
                "        element.style.opacity = '1';" +
                "        element.style.zIndex = '9999';" +
                "        console.log('Unhidden video element: ' + element.tagName + ' ' + element.className);" +
                "      }" +
                "    }" +
                "  }" +
                "  " +
                "  console.log('MultiEmbed black screen fix applied successfully');" +
                "} catch(e) {" +
                "  console.log('MultiEmbed fix error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "MultiEmbed fix with enhanced ad blocking and redirect handling applied: " + value);
                }
            });
        }

        /**
         * Inject JavaScript to fix StreamingNow.mov countdown ads and infinite loops
         */
        private void injectStreamingNowFix(WebView view) {
            String jsCode = "javascript:" +
                "try {" +
                "  console.log('Applying StreamingNow.mov countdown ad fix...');" +
                "  " +
                "  // Aggressive countdown ad removal" +
                "  var countdownSelectors = [" +
                "    '[class*=\"countdown\"]', '[id*=\"countdown\"]', '[class*=\"timer\"]', '[id*=\"timer\"]'," +
                "    '[class*=\"ad\"]', '[id*=\"ad\"]', '[class*=\"ads\"]', '[id*=\"ads\"]'," +
                "    '[class*=\"popup\"]', '[id*=\"popup\"]', '[class*=\"modal\"]', '[id*=\"modal\"]'," +
                "    '[class*=\"overlay\"]', '[id*=\"overlay\"]', '[class*=\"blocker\"]', '[id*=\"blocker\"]'," +
                "    '.countdown-container', '.timer-container', '.ad-container', '.popup-container'," +
                "    '.modal-overlay', '.ad-overlay', '.blocker-overlay', '.countdown-wrapper'," +
                "    '[data-countdown]', '[data-timer]', '[data-ad]', '[data-popup]'" +
                "  ];" +
                "  " +
                "  countdownSelectors.forEach(function(selector) {" +
                "    var elements = document.querySelectorAll(selector);" +
                "    console.log('Found ' + elements.length + ' countdown/ad elements with selector: ' + selector);" +
                "    for(var i = 0; i < elements.length; i++) {" +
                "      elements[i].style.display = 'none';" +
                "      elements[i].style.visibility = 'hidden';" +
                "      elements[i].style.opacity = '0';" +
                "      elements[i].style.pointerEvents = 'none';" +
                "      elements[i].style.zIndex = '-9999';" +
                "      console.log('Blocked countdown/ad element: ' + selector);" +
                "    }" +
                "  });" +
                "  " +
                "  // Remove any countdown timers" +
                "  var timers = document.querySelectorAll('span, div, p');" +
                "  for(var i = 0; i < timers.length; i++) {" +
                "    var text = timers[i].textContent || timers[i].innerText;" +
                "    if(text && (text.includes('00:') || text.includes('0:') || text.match(/\\d+:\\d+/))) {" +
                "      if(timers[i].parentElement && timers[i].parentElement.style.display !== 'none') {" +
                "        timers[i].parentElement.style.display = 'none';" +
                "        console.log('Blocked countdown timer: ' + text);" +
                "      }" +
                "    }" +
                "  }" +
                "  " +
                "  // Force video element to be visible and fix black screen" +
                "  var videos = document.querySelectorAll('video');" +
                "  console.log('Found ' + videos.length + ' video elements');" +
                "  for(var i = 0; i < videos.length; i++) {" +
                "    videos[i].style.display = 'block';" +
                "    videos[i].style.visibility = 'visible';" +
                "    videos[i].style.opacity = '1';" +
                "    videos[i].style.zIndex = '9999';" +
                "    videos[i].style.position = 'relative';" +
                "    videos[i].style.width = '100%';" +
                "    videos[i].style.height = '100%';" +
                "    videos[i].style.backgroundColor = 'black';" +
                "    videos[i].autoplay = true;" +
                "    videos[i].muted = false;" +
                "    videos[i].controls = true;" +
                "    videos[i].loop = false;" +
                "    videos[i].preload = 'auto';" +
                "    videos[i].playsInline = true;" +
                "    videos[i].webkitPlaysinline = true;" +
                "    console.log('Fixed video element ' + i);" +
                "  }" +
                "  " +
                "  // Force iframe elements to be visible" +
                "  var iframes = document.querySelectorAll('iframe');" +
                "  console.log('Found ' + iframes.length + ' iframe elements');" +
                "  for(var i = 0; i < iframes.length; i++) {" +
                "    iframes[i].style.display = 'block';" +
                "    iframes[i].style.visibility = 'visible';" +
                "    iframes[i].style.opacity = '1';" +
                "    iframes[i].style.zIndex = '9999';" +
                "    iframes[i].style.position = 'relative';" +
                "    iframes[i].style.width = '100%';" +
                "    iframes[i].style.height = '100%';" +
                "    iframes[i].style.backgroundColor = 'black';" +
                "    iframes[i].style.border = 'none';" +
                "    iframes[i].style.margin = '0';" +
                "    iframes[i].style.padding = '0';" +
                "    console.log('Fixed iframe element ' + i);" +
                "  }" +
                "  " +
                "  // Remove any overlay elements that might be blocking the video" +
                "  var overlays = document.querySelectorAll('.overlay, .ad-overlay, .blocker, .ads, .advertisement, [class*=\"overlay\"], [class*=\"blocker\"]');" +
                "  console.log('Found ' + overlays.length + ' overlay elements');" +
                "  for(var i = 0; i < overlays.length; i++) {" +
                "    overlays[i].style.display = 'none';" +
                "    overlays[i].style.visibility = 'hidden';" +
                "    overlays[i].style.opacity = '0';" +
                "    overlays[i].style.pointerEvents = 'none';" +
                "    overlays[i].style.zIndex = '-9999';" +
                "    console.log('Removed overlay element ' + i);" +
                "  }" +
                "  " +
                "  // Force body background to be transparent" +
                "  document.body.style.backgroundColor = 'transparent';" +
                "  document.body.style.background = 'transparent';" +
                "  " +
                "  // Force html background to be transparent" +
                "  document.documentElement.style.backgroundColor = 'transparent';" +
                "  document.documentElement.style.background = 'transparent';" +
                "  " +
                "  // Disable any ad-related scripts" +
                "  var scripts = document.querySelectorAll('script');" +
                "  for(var i = 0; i < scripts.length; i++) {" +
                "    if(scripts[i].src && (scripts[i].src.includes('ad') || scripts[i].src.includes('ads') || scripts[i].src.includes('analytics') || scripts[i].src.includes('countdown') || scripts[i].src.includes('timer'))) {" +
                "      scripts[i].remove();" +
                "      console.log('Removed ad/countdown script: ' + scripts[i].src);" +
                "    }" +
                "  }" +
                "  " +
                "  // Force autoplay for any video elements" +
                "  setTimeout(function() {" +
                "    var videos = document.querySelectorAll('video');" +
                "    for(var i = 0; i < videos.length; i++) {" +
                "      if(videos[i].paused) {" +
                "        videos[i].play().catch(function(e) {" +
                "          console.log('Autoplay failed for video ' + i + ': ' + e.message);" +
                "        });" +
                "      }" +
                "    }" +
                "  }, 1000);" +
                "  " +
                "  // Additional aggressive fix - look for any hidden elements that might be video players" +
                "  var allElements = document.querySelectorAll('*');" +
                "  for(var i = 0; i < allElements.length; i++) {" +
                "    var element = allElements[i];" +
                "    if(element.style.display === 'none' || element.style.visibility === 'hidden') {" +
                "      if(element.tagName === 'VIDEO' || element.tagName === 'IFRAME' || " +
                "         element.className.includes('player') || element.id.includes('player') || " +
                "         element.className.includes('video') || element.id.includes('video')) {" +
                "        element.style.display = 'block';" +
                "        element.style.visibility = 'visible';" +
                "        element.style.opacity = '1';" +
                "        element.style.zIndex = '9999';" +
                "        console.log('Unhidden video element: ' + element.tagName + ' ' + element.className);" +
                "      }" +
                "    }" +
                "  }" +
                "  " +
                "  console.log('StreamingNow.mov countdown ad fix applied successfully');" +
                "} catch(e) {" +
                "  console.log('StreamingNow.mov fix error: ' + e.message);" +
                "}";

            view.evaluateJavascript(jsCode, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Log.d(TAG, "StreamingNow.mov countdown ad fix applied: " + value);
                }
            });
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e(TAG, "WebView error: " + errorCode + " - " + description + " for URL: " + failingUrl);

            loadAttempts++;
            if (loadAttempts < MAX_ATTEMPTS) {
                // Try fallback server
                String fallbackUrl = VideoServerUtils.getFallbackUrl(failingUrl, loadAttempts - 1);
                if (fallbackUrl != null && !fallbackUrl.equals(failingUrl)) {
                    Log.d(TAG, "Trying fallback URL: " + fallbackUrl);
                    view.loadUrl(fallbackUrl);
                    return;
                }
            }

            // Show error message to user
            Toast.makeText(EmbedActivity.this,
                "Video server unavailable. Please try a different source.",
                Toast.LENGTH_LONG).show();

            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            Log.e(TAG, "HTTP Error: " + errorResponse.getStatusCode() + " for URL: " + request.getUrl());

            loadAttempts++;
            if (loadAttempts < MAX_ATTEMPTS) {
                // Try fallback server
                String fallbackUrl = VideoServerUtils.getFallbackUrl(request.getUrl().toString(), loadAttempts - 1);
                if (fallbackUrl != null && !fallbackUrl.equals(request.getUrl().toString())) {
                    Log.d(TAG, "Trying fallback URL: " + fallbackUrl);
                    view.loadUrl(fallbackUrl);
                    return;
                }
            }

            // Show error message to user
            Toast.makeText(EmbedActivity.this,
                "Video server unavailable. Please try a different source.",
                Toast.LENGTH_LONG).show();

            super.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.e(TAG, "SSL Error: " + error.toString());
            // Continue loading despite SSL errors for video servers (CinemaX approach)
            handler.proceed();
        }

        /**
         * Helper method for domain validation (CinemaX approach)
         */
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
}