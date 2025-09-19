package com.cinecraze.free.utils;

import android.util.Log;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility class for handling video server configurations and fallbacks
 */
public class VideoServerUtils {
    
    private static final String TAG = "VideoServerUtils";
    
    // Video server configurations
    public static final String VIDSRC_BASE = "https://vidsrc.to/embed";
    public static final String VIDSRC_NET_BASE = "https://vidsrc.net/embed";
    public static final String VIDJOY_BASE = "https://vidjoy.pro/embed";
    public static final String VIDBINGE_BASE = "https://vidbinge.dev/embed";
    public static final String MULTIEMBED_BASE = "https://multiembed.mov";
    public static final String SUPEREMBED_BASE = "https://superembed.mov";
    
    // New video hosting platforms
    public static final String YOUTUBE_BASE = "https://www.youtube.com";
    public static final String YOUTUBE_EMBED_BASE = "https://www.youtube.com/embed";
    public static final String GOOGLE_DRIVE_BASE = "https://drive.google.com";
    public static final String MEGA_BASE = "https://mega.nz";
    // Additional embed bases
    public static final String VIDSRC_XYZ_BASE = "https://vidsrc.xyz/embed";
    public static final String EMBED_SU_BASE = "https://embed.su/embed";
    public static final String VIDLINK_PRO_BASE = "https://vidlink.pro";
    public static final String AUTOEMBED_BASE = "https://player.autoembed.cc/embed";
    
    // Popular embedded video servers (including common variations)
    public static final String[] EMBED_SERVERS = {
        "vidsrc.to", "vidsrc.net", "vidjoy.pro", "vidbinge.dev", "multiembed.mov", "superembed.mov",
        "youtube.com", "youtu.be", "drive.google.com", "mega.nz",
        "streamwish.to", "doodstream.com", "mixdrop.co", "streamtape.com",
        "vidcloud.co", "upcloud.to", "nova.video", "streamhub.to",
        // Added
        "vidsrc.xyz", "vidsrc.me", "embed.su", "vidlink.pro", "autoembed.cc", "player.autoembed.cc"
    };
    
    // Video quality options
    public static final String[] VIDEO_QUALITIES = {"1080p", "720p", "480p", "360p"};
    
    // Video format patterns
    private static final Pattern MP4_PATTERN = Pattern.compile(".*\\.(mp4|m4v).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern HLS_PATTERN = Pattern.compile(".*\\.(m3u8|hls).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DASH_PATTERN = Pattern.compile(".*\\.(mpd|dash).*", Pattern.CASE_INSENSITIVE);
    
    // URL patterns for different platforms
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([a-zA-Z0-9_-]{11})");
    private static final Pattern GOOGLE_DRIVE_PATTERN = Pattern.compile("drive\\.google\\.com/(?:file/d/|open\\?id=)([a-zA-Z0-9_-]+)");
    private static final Pattern MEGA_PATTERN = Pattern.compile("mega\\.nz/(?:file/|folder/)([a-zA-Z0-9_-]+)");
    
    /**
     * Get video type from URL to determine proper MediaSource
     */
    public static String getVideoType(String url) {
        if (url == null) return "mp4";
        
        url = url.toLowerCase();
        
        if (url.contains(".m3u8") || url.contains("hls")) {
            return "m3u8";
        } else if (url.contains(".mpd") || url.contains("dash")) {
            return "dash";
        } else if (url.contains(".mp4")) {
            return "mp4";
        } else if (url.contains(".mkv")) {
            return "mp4"; // Use MP4 source for MKV as well
        } else if (url.contains(".webm")) {
            return "mp4"; // Use MP4 source for WebM
        } else if (url.contains(".avi")) {
            return "mp4"; // Use MP4 source for AVI
        }
        
        // Default to MP4 for unknown formats
        return "mp4";
    }
    
    /**
     * Check if URL is an embedded video server
     */
    public static boolean isEmbeddedVideoUrl(String url) {
        if (url == null) return false;
        
        for (String server : EMBED_SERVERS) {
            if (url.contains(server)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Enhance video URL with fallback parameters
     */
    public static String enhanceVideoUrl(String originalUrl) {
        if (originalUrl == null) return originalUrl;
        
        // Handle different embed servers
        if (originalUrl.contains("vidsrc.to") || originalUrl.contains("vidsrc.net")) {
            return addVidSrcParameters(originalUrl);
        } else if (originalUrl.contains("vidjoy.pro")) {
            return addVidJoyParameters(originalUrl);
        } else if (originalUrl.contains("vidbinge.dev")) {
            return addVidBingeParameters(originalUrl);
        } else if (originalUrl.contains("multiembed.mov")) {
            return addMultiembedParameters(originalUrl);
        } else if (originalUrl.contains("superembed.mov")) {
            return addSuperEmbedParameters(originalUrl);
        } else if (isYouTubeUrl(originalUrl)) {
            return addYouTubeParameters(originalUrl);
        } else if (isGoogleDriveUrl(originalUrl)) {
            return addGoogleDriveParameters(originalUrl);
        } else if (isMegaUrl(originalUrl)) {
            return addMegaParameters(originalUrl);
        } else if (isMultiEmbedUrl(originalUrl)) {
            return addMultiembedParameters(originalUrl);
        } else if (isMpdUrl(originalUrl)) {
            return addMpdParameters(originalUrl);
        } else if (originalUrl.contains("vidsrc.xyz")) {
            return addVidsrcXyzParameters(originalUrl);
        } else if (originalUrl.contains("embed.su")) {
            return addEmbedSuParameters(originalUrl);
        } else if (originalUrl.contains("vidlink.pro")) {
            return addVidlinkProParameters(originalUrl);
        } else if (originalUrl.contains("autoembed.cc") || originalUrl.contains("player.autoembed.cc")) {
            return addAutoembedParameters(originalUrl);
        }
        
        return originalUrl;
    }
    
    /**
     * Check if URL is a YouTube URL
     */
    public static boolean isYouTubeUrl(String url) {
        return url != null && (url.contains("youtube.com") || url.contains("youtu.be"));
    }
    
    /**
     * Check if URL is a Google Drive URL
     */
    public static boolean isGoogleDriveUrl(String url) {
        return url != null && url.contains("drive.google.com");
    }
    
    /**
     * Check if URL is a Mega URL
     */
    public static boolean isMegaUrl(String url) {
        return url != null && url.contains("mega.nz");
    }
    
    /**
     * Check if URL is a MultiEmbed URL
     */
    public static boolean isMultiEmbedUrl(String url) {
        return url != null && (url.contains("multiembed.mov") || url.contains("streamingnow.mov"));
    }
    
    /**
     * Check if URL is a MultiEmbed redirect URL (streamingnow.mov)
     */
    public static boolean isMultiEmbedRedirectUrl(String url) {
        return url != null && url.contains("streamingnow.mov");
    }
    
    /**
     * Check if URL is the actual streaming source (streamingnow.mov)
     */
    public static boolean isStreamingNowUrl(String url) {
        return url != null && url.contains("streamingnow.mov");
    }
    
    /**
     * Check if URL is a DASH/MPD stream that requires Shaka Player
     */
    public static boolean isMpdUrl(String url) {
        if (url == null) return false;
        
        return url.endsWith(".mpd") || 
               url.contains(".mpd?") || 
               url.contains("dash") || 
               url.contains("DASH") ||
               url.contains("manifest.mpd");
    }
    
    /**
     * Check if URL is a DRM-protected stream
     */
    public static boolean isDrmProtectedUrl(String url) {
        if (url == null) return false;
        
        return url.contains("drm") || 
               url.contains("DRM") ||
               url.contains("widevine") || 
               url.contains("playready") ||
               url.contains("fairplay") || 
               url.contains("clearkey");
    }
    
    /**
     * Extract YouTube video ID from URL
     */
    public static String extractYouTubeVideoId(String url) {
        if (url == null) return null;
        
        Matcher matcher = YOUTUBE_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract Google Drive file ID from URL
     */
    public static String extractGoogleDriveFileId(String url) {
        if (url == null) return null;
        
        Matcher matcher = GOOGLE_DRIVE_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract Mega file/folder ID from URL
     */
    public static String extractMegaId(String url) {
        if (url == null) return null;
        
        Matcher matcher = MEGA_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Add VidSrc.to parameters for better reliability
     */
    private static String addVidSrcParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("server=vidcloud")
                  .append("&quality=1080p")
                  .append("&autoplay=1")
                  .append("&t=1");
        
        Log.d(TAG, "Enhanced VidSrc URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add VidJoy.pro parameters for better reliability
     */
    private static String addVidJoyParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("quality=1080p")
                  .append("&server=auto")
                  .append("&autoplay=1")
                  .append("&sub.file=");
        
        Log.d(TAG, "Enhanced VidJoy URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add VidBinge.dev parameters
     */
    private static String addVidBingeParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        enhancedUrl.append("quality=auto")
                  .append("&autoplay=1")
                  .append("&sub=1");
        
        Log.d(TAG, "Enhanced VidBinge URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add Multiembed parameters (SuperEmbed alternative)
     */
    private static String addMultiembedParameters(String url) {
        // For multiembed, we need to handle redirects properly
        // The original URL redirects to streamingnow.mov, so we should follow that redirect
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        // Basic parameters for MultiEmbed - avoid YouTube-specific parameters
        enhancedUrl.append("autoplay=1")
                  .append("&muted=0")
                  .append("&controls=1");
        
        Log.d(TAG, "Enhanced Multiembed URL (with redirect handling): " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    // New: Add simple enhancement parameters for additional domains
    private static String addVidsrcXyzParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        enhancedUrl.append("autoplay=1");
        Log.d(TAG, "Enhanced VidSrc.xyz URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    private static String addEmbedSuParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        enhancedUrl.append("autoplay=1");
        Log.d(TAG, "Enhanced Embed.su URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    private static String addVidlinkProParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        enhancedUrl.append("autoplay=1");
        Log.d(TAG, "Enhanced VidLink.pro URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    private static String addAutoembedParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        enhancedUrl.append("autoplay=1");
        Log.d(TAG, "Enhanced AutoEmbed URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add SuperEmbed parameters for better reliability and to fix black screen issue
     */
    private static String addSuperEmbedParameters(String url) {
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        // SuperEmbed specific parameters to fix black screen issue
        enhancedUrl.append("autoplay=1")
                  .append("&muted=0")
                  .append("&controls=1")
                  .append("&rel=0")
                  .append("&showinfo=0")
                  .append("&iv_load_policy=3")
                  .append("&modestbranding=1")
                  .append("&playsinline=1")
                  .append("&enablejsapi=1")
                  .append("&origin=" + encodeUrl("https://superembed.mov"))
                  .append("&widget_referrer=" + encodeUrl("https://superembed.mov"));
        
        Log.d(TAG, "Enhanced SuperEmbed URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add YouTube parameters for better embedding
     */
    private static String addYouTubeParameters(String url) {
        String videoId = extractYouTubeVideoId(url);
        if (videoId == null) return url;
        
        // Convert to embed URL
        String embedUrl = YOUTUBE_EMBED_BASE + "/" + videoId;
        
        StringBuilder enhancedUrl = new StringBuilder(embedUrl);
        enhancedUrl.append("?autoplay=1")
                  .append("&muted=0")
                  .append("&controls=1")
                  .append("&rel=0")
                  .append("&showinfo=0")
                  .append("&iv_load_policy=3")
                  .append("&modestbranding=1")
                  .append("&playsinline=1")
                  .append("&enablejsapi=1")
                  .append("&origin=" + encodeUrl("https://www.youtube.com"))
                  .append("&widget_referrer=" + encodeUrl("https://www.youtube.com"));
        
        Log.d(TAG, "Enhanced YouTube URL: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add Google Drive parameters for better embedding
     */
    private static String addGoogleDriveParameters(String url) {
        String fileId = extractGoogleDriveFileId(url);
        if (fileId == null) return url;
        
        // Convert to embed URL with parameters to hide interface
        String embedUrl = GOOGLE_DRIVE_BASE + "/file/d/" + fileId + "/preview";
        
        // Add parameters to hide Google Drive interface elements
        StringBuilder enhancedUrl = new StringBuilder(embedUrl);
        enhancedUrl.append("?usp=sharing")
                  .append("&rm=minimal")
                  .append("&ui=2")
                  .append("&chrome=false");
        
        Log.d(TAG, "Enhanced Google Drive URL for pure video player: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * Add Mega parameters for better embedding
     */
    private static String addMegaParameters(String url) {
        // For Mega, we need to force embed mode and hide all interface elements
        StringBuilder enhancedUrl = new StringBuilder(url);
        
        if (!url.contains("?")) {
            enhancedUrl.append("?");
        } else {
            enhancedUrl.append("&");
        }
        
        // Force embed mode and hide all Mega interface elements
        enhancedUrl.append("embed=1")
                  .append("&autoplay=1")
                  .append("&muted=0")
                  .append("&controls=1")
                  .append("&playsinline=1")
                  .append("&hide_ui=1")
                  .append("&minimal=1")
                  .append("&no_header=1")
                  .append("&no_footer=1")
                  .append("&no_sidebar=1");
        
        Log.d(TAG, "Enhanced Mega URL for pure video player: " + enhancedUrl.toString());
        return enhancedUrl.toString();
    }
    
    /**
     * URL encode helper method
     */
    private static String encodeUrl(String url) {
        try {
            return java.net.URLEncoder.encode(url, "UTF-8");
        } catch (Exception e) {
            return url;
        }
    }
    
    /**
     * Get fallback URL when primary server fails
     */
    public static String getFallbackUrl(String failingUrl, int attempt) {
        if (failingUrl == null) return null;
        
        if (failingUrl.contains("vidsrc.to") || failingUrl.contains("vidsrc.net")) {
            return getVidSrcFallback(failingUrl, attempt);
        } else if (failingUrl.contains("vidjoy.pro")) {
            return getVidJoyFallback(failingUrl, attempt);
        } else if (failingUrl.contains("vidbinge.dev")) {
            return getVidBingeFallback(failingUrl, attempt);
        } else if (failingUrl.contains("superembed.mov")) {
            return getSuperEmbedFallback(failingUrl, attempt);
        } else if (isYouTubeUrl(failingUrl)) {
            return getYouTubeFallback(failingUrl, attempt);
        } else if (isGoogleDriveUrl(failingUrl)) {
            return getGoogleDriveFallback(failingUrl, attempt);
        } else if (isMegaUrl(failingUrl)) {
            return getMegaFallback(failingUrl, attempt);
        } else if (isMultiEmbedUrl(failingUrl)) {
            return getMultiEmbedFallback(failingUrl, attempt);
        }
        
        return null;
    }
    
    /**
     * Get VidSrc fallback URL
     */
    private static String getVidSrcFallback(String url, int attempt) {
        String[] servers = {"vidcloud", "upcloud", "nova", "streamwish"};
        if (attempt >= servers.length) return null;
        
        String server = servers[attempt];
        if (url.contains("server=")) {
            return url.replaceAll("server=[^&]*", "server=" + server);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "server=" + server;
        }
    }
    
    /**
     * Get VidJoy fallback URL
     */
    private static String getVidJoyFallback(String url, int attempt) {
        if (attempt >= VIDEO_QUALITIES.length) return null;
        
        String quality = VIDEO_QUALITIES[attempt];
        if (url.contains("quality=")) {
            return url.replaceAll("quality=[^&]*", "quality=" + quality);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "quality=" + quality;
        }
    }
    
    /**
     * Get VidBinge fallback URL
     */
    private static String getVidBingeFallback(String url, int attempt) {
        String[] qualities = {"auto", "1080p", "720p", "480p"};
        if (attempt >= qualities.length) return null;
        
        String quality = qualities[attempt];
        if (url.contains("quality=")) {
            return url.replaceAll("quality=[^&]*", "quality=" + quality);
        } else {
            return url + (url.contains("?") ? "&" : "?") + "quality=" + quality;
        }
    }
    
    /**
     * Get SuperEmbed fallback URL with different parameters to fix black screen
     */
    private static String getSuperEmbedFallback(String url, int attempt) {
        // If SuperEmbed is down, try MultiEmbed as alternative
        if (attempt == 0) {
            // Try to convert SuperEmbed URL to MultiEmbed URL
            String videoId = extractVideoId(url);
            if (videoId != null) {
                Log.d(TAG, "SuperEmbed down, trying MultiEmbed alternative for video ID: " + videoId);
                return MULTIEMBED_BASE + "/directstream.php?video_id=" + videoId + "&tmdb=1&server=vip&quality=1080p&autoplay=1&muted=0&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1&enablejsapi=1&origin=" + encodeUrl("https://multiembed.mov") + "&widget_referrer=" + encodeUrl("https://multiembed.mov") + "&allowfullscreen=1&allowscriptaccess=always&wmode=transparent";
            }
        }
        
        String[] fallbackParams = {
            "autoplay=1&muted=0&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1&enablejsapi=1",
            "autoplay=1&muted=1&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1",
            "autoplay=0&muted=0&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1",
            "autoplay=1&muted=0&controls=0&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1"
        };
        
        if (attempt >= fallbackParams.length) return null;
        
        String params = fallbackParams[attempt];
        
        // Remove existing parameters and add new ones
        String baseUrl = url.split("\\?")[0];
        return baseUrl + "?" + params;
    }
    
    /**
     * Get YouTube fallback URL with different parameters
     */
    private static String getYouTubeFallback(String url, int attempt) {
        String videoId = extractYouTubeVideoId(url);
        if (videoId == null) return null;
        
        String[] fallbackParams = {
            "autoplay=1&muted=0&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1&enablejsapi=1",
            "autoplay=1&muted=1&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1",
            "autoplay=0&muted=0&controls=1&rel=0&showinfo=0&iv_load_policy=3&modestbranding=1&playsinline=1"
        };
        
        if (attempt >= fallbackParams.length) return null;
        
        String params = fallbackParams[attempt];
        return YOUTUBE_EMBED_BASE + "/" + videoId + "?" + params;
    }
    
    /**
     * Get Google Drive fallback URL
     */
    private static String getGoogleDriveFallback(String url, int attempt) {
        String fileId = extractGoogleDriveFileId(url);
        if (fileId == null) return null;
        
        // Google Drive doesn't have many fallback options, but we can try different embed methods
        String[] fallbackMethods = {
            "/preview",
            "/view",
            "/uc?export=view"
        };
        
        if (attempt >= fallbackMethods.length) return null;
        
        return GOOGLE_DRIVE_BASE + "/file/d/" + fileId + fallbackMethods[attempt];
    }
    
    /**
     * Get Mega fallback URL
     */
    private static String getMegaFallback(String url, int attempt) {
        // Mega URLs are typically direct, so we return the original
        return url;
    }

    /**
     * Get MultiEmbed fallback URL (SuperEmbed alternative)
     */
    private static String getMultiEmbedFallback(String url, int attempt) {
        String[] fallbackParams = {
            "autoplay=1&muted=0&controls=1",
            "autoplay=1&muted=1&controls=1",
            "autoplay=0&muted=0&controls=1"
        };

        if (attempt >= fallbackParams.length) return null;

        String params = fallbackParams[attempt];
        return MULTIEMBED_BASE + "/directstream.php?video_id=" + extractVideoId(url) + "&tmdb=1&" + params;
    }
    
    /**
     * Build embed URL for TMDB/IMDB ID
     */
    public static String buildEmbedUrl(String server, String tmdbId, String imdbId, String type, int season, int episode) {
        switch (server.toLowerCase()) {
            case "vidsrc":
                if (type.equals("movie")) {
                    return VIDSRC_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDSRC_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "vidsrcnet":
                if (type.equals("movie")) {
                    return VIDSRC_NET_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDSRC_NET_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "vidjoy":
                if (type.equals("movie")) {
                    return VIDJOY_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDJOY_BASE + "/tv/" + tmdbId + "-" + season + "-" + episode;
                }
            case "vidbinge":
                if (type.equals("movie")) {
                    return VIDBINGE_BASE + "/movie?tmdb=" + tmdbId;
                } else {
                    return VIDBINGE_BASE + "/tv?tmdb=" + tmdbId + "&season=" + season + "&episode=" + episode;
                }
            case "multiembed":
                if (type.equals("movie")) {
                    return MULTIEMBED_BASE + "/directstream.php?video_id=" + tmdbId + "&tmdb=1";
                } else {
                    return MULTIEMBED_BASE + "/directstream.php?video_id=" + tmdbId + "&tmdb=1&s=" + season + "&e=" + episode;
                }
            case "superembed":
                if (type.equals("movie")) {
                    return SUPEREMBED_BASE + "/movie/" + tmdbId;
                } else {
                    return SUPEREMBED_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "vidsrcxyz":
                if (type.equals("movie")) {
                    return VIDSRC_XYZ_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDSRC_XYZ_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "embedsu":
                if (type.equals("movie")) {
                    return EMBED_SU_BASE + "/movie/" + tmdbId;
                } else {
                    return EMBED_SU_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "vidlink":
            case "vidlinkpro":
                if (type.equals("movie")) {
                    return VIDLINK_PRO_BASE + "/movie/" + tmdbId;
                } else {
                    return VIDLINK_PRO_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            case "autoembed":
                if (type.equals("movie")) {
                    return AUTOEMBED_BASE + "/movie/" + tmdbId;
                } else {
                    return AUTOEMBED_BASE + "/tv/" + tmdbId + "/" + season + "/" + episode;
                }
            default:
                return null;
        }
    }
    
    /**
     * Build direct URL for external platforms
     */
    public static String buildDirectUrl(String platform, String videoId, String type, int season, int episode) {
        switch (platform.toLowerCase()) {
            case "youtube":
                return YOUTUBE_EMBED_BASE + "/" + videoId;
            case "googledrive":
            case "gdrive":
                return GOOGLE_DRIVE_BASE + "/file/d/" + videoId + "/preview";
            case "mega":
                return MEGA_BASE + "/file/" + videoId;
            default:
                return null;
        }
    }
    
    /**
     * Check if URL is from a supported video server
     */
    public static boolean isSupportedVideoServer(String url) {
        if (url == null) return false;
        
        for (String server : EMBED_SERVERS) {
            if (url.contains(server)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get server type from URL
     */
    public static String getServerType(String url) {
        if (url == null) return "unknown";
        
        if (url.contains("vidsrc.to")) return "VidSrc.to";
        if (url.contains("vidsrc.net")) return "VidSrc.net";
        if (url.contains("vidjoy.pro")) return "VidJoy";
        if (url.contains("vidbinge.dev")) return "VidBinge";
        if (url.contains("multiembed.mov")) return "MultiEmbed";
        if (url.contains("superembed.mov")) return "SuperEmbed";
        if (url.contains("youtube.com") || url.contains("youtu.be")) return "YouTube";
        if (url.contains("drive.google.com")) return "Google Drive";
        if (url.contains("mega.nz")) return "Mega";
        if (url.contains("streamwish.to")) return "StreamWish";
        if (url.contains("doodstream.com")) return "DoodStream";
        if (url.contains("mixdrop.co")) return "MixDrop";
        if (url.contains("streamtape.com")) return "StreamTape";
        // Added
        if (url.contains("vidsrc.xyz")) return "VidSrc.xyz";
        if (url.contains("embed.su")) return "Embed.su";
        if (url.contains("vidlink.pro")) return "VidLink.pro";
        if (url.contains("autoembed.cc") || url.contains("player.autoembed.cc")) return "AutoEmbed";
        
        for (String server : EMBED_SERVERS) {
            if (url.contains(server)) {
                return server.split("\\.")[0]; // Get first part before domain
            }
        }
        
        return "unknown";
    }
    
    /**
     * Validate video URL format
     */
    public static boolean isValidVideoUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        
        return url.startsWith("http://") || url.startsWith("https://") || 
               url.startsWith("rtmp://") || url.startsWith("rtmps://");
    }
    
    /**
     * Extract video ID from various URL formats
     */
    public static String extractVideoId(String url) {
        if (url == null) return null;
        
        // Extract TMDB ID pattern
        Pattern tmdbPattern = Pattern.compile("(?:tmdb|movie|tv)/(\\d+)");
        Matcher matcher = tmdbPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Extract IMDB ID pattern
        Pattern imdbPattern = Pattern.compile("(tt\\d+)");
        matcher = imdbPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Extract YouTube video ID
        String youtubeId = extractYouTubeVideoId(url);
        if (youtubeId != null) {
            return youtubeId;
        }
        
        // Extract Google Drive file ID
        String driveId = extractGoogleDriveFileId(url);
        if (driveId != null) {
            return driveId;
        }
        
        // Extract Mega ID
        String megaId = extractMegaId(url);
        if (megaId != null) {
            return megaId;
        }
        
        return null;
    }
    
    /**
     * Get user agent for better compatibility
     */
    public static String getUserAgent() {
        return "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36";
    }
    
    /**
     * Add parameters for MPD/DASH streams
     */
    private static String addMpdParameters(String url) {
        // For MPD files, we typically don't need to modify the URL
        // The enhancement will be handled by Shaka Player injection
        return url;
    }

    public static boolean isNetworkUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        return url.startsWith("http://") || url.startsWith("https://") ||
               url.startsWith("rtmp://") || url.startsWith("rtmps://");
    }

    /**
     * Check if URL points to a direct downloadable media file (not embed/stream playlist)
     */
    public static boolean isDirectFileUrl(String url) {
        if (!isNetworkUrl(url)) return false;
        if (isEmbeddedVideoUrl(url)) return false;
        String lower = url.toLowerCase();
        // Prefer file-based formats for DownloadManager
        return lower.contains(".mp4") || lower.contains(".mkv") || lower.contains(".webm") || lower.contains(".avi");
    }
}