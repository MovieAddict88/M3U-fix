package com.cinecraze.free.player;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
// import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection; // Removed - no longer used
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.cinecraze.free.utils.VideoServerUtils;

import java.util.ArrayList;

public class CustomPlayerViewModel implements Player.Listener {
    
    private static final String TAG = "CustomPlayerViewModel";
    private ArrayList<CustomPlayerFragment.Subtitle> subtitlesForCast = new ArrayList<>();

    private Activity mActivity;
    private String mUrl;
    private boolean loadingComplete = false;
    private static final boolean SHOULD_AUTO_PLAY = true;
    private boolean mPausable = true;
    private boolean isInProgress = false;
    private boolean isLoadingNow = false;

    private PlayerView mPlayerView;
    public ExoPlayer mExoPlayer;
    private RelativeLayout payer_pause_play;
    private Boolean isLive = false;
    private String videoType;
    private String videoImage;
    private String videoTitle;
    private String videoSubTile;

    public CustomPlayerViewModel(Activity activity) {
        mActivity = activity;
    }

    public void onStart(PlayerView playerView, Bundle bundle) {
        if (bundle == null) {
            Log.e(TAG, "Bundle is null");
            return;
        }
        
        mPlayerView = playerView;
        mUrl = bundle.getString("videoUrl");
        isLive = bundle.getBoolean("isLive", false);
        videoType = bundle.getString("videoType");
        videoTitle = bundle.getString("videoTitle");
        videoSubTile = bundle.getString("videoSubTile");
        videoImage = bundle.getString("videoImage");
        
        // Validate required data
        if (mUrl == null || mUrl.isEmpty()) {
            Log.e(TAG, "Video URL is null or empty");
            return;
        }
        
        // Determine video type from URL if not provided
        if (videoType == null || videoType.isEmpty()) {
            videoType = VideoServerUtils.getVideoType(mUrl);
        }
        
        initPlayer();
        if (mPlayerView != null) {
            mPlayerView.setPlayer(mExoPlayer);
        }

        preparePlayer(null, 0);
    }

    public void setPayerPausePlay(RelativeLayout payer_pause_play) {
        this.payer_pause_play = payer_pause_play;
    }
    
    public void setMediaFull() {
        if (mPlayerView != null) {
            mPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        }
        if (mExoPlayer != null) {
            mExoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        }
    }
    
    public void setMediaNormal() {
        if (mPlayerView != null) {
            mPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }
    }
    
    private void initPlayer() {
        if (mActivity == null) {
            Log.e(TAG, "Activity is null");
            return;
        }
        
        try {
            // Create a default TrackSelector
            Handler mainHandler = new Handler();
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(mActivity).build();
            TrackSelector trackSelector = new DefaultTrackSelector(mActivity);

            // Create a default LoadControl
            LoadControl loadControl = new DefaultLoadControl();

            // Create the player
            mExoPlayer = new ExoPlayer.Builder(mActivity)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build();
            
            mExoPlayer.addListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error creating ExoPlayer: " + e.getMessage());
        }
    }

    public void preparePlayer(CustomPlayerFragment.Subtitle subtitle, long seekTo) {
        // Validate URL
        if (mUrl == null || mUrl.isEmpty()) {
            Log.e(TAG, "Video URL is null or empty");
            return;
        }

        try {
            // Create data source factory
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(mActivity).build();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mActivity,
                    Util.getUserAgent(mActivity, "CineCraze"), bandwidthMeter);
            
            Uri videoUri = Uri.parse(mUrl);
            MediaSource mediaSource1;
            int sourceSize = 1;

            // Create appropriate media source based on video type
            if (videoType == null) {
                videoType = VideoServerUtils.getVideoType(mUrl);
            }
            
            switch (videoType) {
                case "m3u8":
                case "hls":
                    mediaSource1 = new HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(videoUri));
                    break;
                case "dash":
                    mediaSource1 = new DashMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(videoUri));
                    break;
                case "mp4":
                default:
                    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                    mediaSource1 = new ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(videoUri));
                    break;
            }

            // Handle subtitles if provided
            SingleSampleMediaSource subtitleSource = null;
            if (subtitle != null && subtitle.getUrl() != null) {
                String subtitleType = subtitle.getType() != null ? subtitle.getType() : "srt";
                
                if (subtitleType.equals("srt")) {
                    subtitleSource = new SingleSampleMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.getUrl()))
                                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                                    .setLanguage(subtitle.getLanguage())
                                    .build(), C.TIME_UNSET);
                } else if (subtitleType.equals("vtt")) {
                    subtitleSource = new SingleSampleMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.getUrl()))
                                    .setMimeType(MimeTypes.TEXT_VTT)
                                    .setLanguage(subtitle.getLanguage())
                                    .build(), C.TIME_UNSET);
                } else if (subtitleType.equals("ass")) {
                    subtitleSource = new SingleSampleMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.getUrl()))
                                    .setMimeType(MimeTypes.TEXT_SSA)
                                    .setLanguage(subtitle.getLanguage())
                                    .build(), C.TIME_UNSET);
                }
                
                if (subtitleSource != null) {
                    sourceSize++;
                }
            }

            // Merge media sources
            MediaSource finalMediaSource;
            if (subtitleSource != null) {
                MediaSource[] mediaSources = new MediaSource[sourceSize];
                mediaSources[0] = mediaSource1;
                mediaSources[1] = subtitleSource;
                finalMediaSource = new MergingMediaSource(mediaSources);
            } else {
                finalMediaSource = mediaSource1;
            }

            // Prepare the player
            if (mExoPlayer != null) {
                mExoPlayer.setMediaSource(finalMediaSource);
                mExoPlayer.seekTo(seekTo);
                mExoPlayer.prepare();
                mExoPlayer.setPlayWhenReady(SHOULD_AUTO_PLAY);
            } else {
                Log.e(TAG, "ExoPlayer is null");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in preparePlayer: " + e.getMessage());
        }
    }
    


    public void play() {
        if (mExoPlayer == null) {
            return;
        }
        mExoPlayer.setPlayWhenReady(true);
    }

    public void pause() {
        if (mExoPlayer == null) {
            return;
        }
        mExoPlayer.setPlayWhenReady(false);
    }

    private boolean isPlaying() {
        return mExoPlayer != null && mExoPlayer.getPlayWhenReady();
    }

    private boolean isPausable() {
        return mPausable;
    }

    public void setPausable(boolean pausable) {
        mPausable = pausable;
    }

    public void seekForward(int milliseconds) {
        if (mExoPlayer != null) {
            long newPosition = mExoPlayer.getCurrentPosition() + milliseconds;
            mExoPlayer.seekTo(newPosition);
        }
    }

    public void seekBackward(int milliseconds) {
        if (mExoPlayer != null) {
            long newPosition = mExoPlayer.getCurrentPosition() - milliseconds;
            if (newPosition < 0) newPosition = 0;
            mExoPlayer.seekTo(newPosition);
        }
    }

    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            setMediaFull();
        } else {
            setMediaNormal();
        }
    }

    public void releasePlayer() {
        release();
    }

    public ExoPlayer getExoPlayer() {
        return mExoPlayer;
    }

    public PlayerView getPlayerView() {
        return mPlayerView;
    }

    public void setIsInProgress(boolean isInProgress) {
        this.isInProgress = isInProgress;
    }

    public boolean getLoadingComplete() {
        return loadingComplete;
    }
    
    public boolean isLoadingNow() {
        Log.i(TAG, "ExoPlayer Loading State: " + isLoadingNow);
        if (payer_pause_play != null) {
            if (isLoadingNow) {
                payer_pause_play.setVisibility(View.GONE);
            } else {
                payer_pause_play.setVisibility(View.VISIBLE);
            }
        }
        return isLoadingNow;
    }
    
    private void setLoadingComplete(boolean complete) {
        loadingComplete = complete;
    }

    public void setSubtitilesList(ArrayList<CustomPlayerFragment.Subtitle> _subtitlesForCast) {
        subtitlesForCast = _subtitlesForCast;
    }

    // Player.Listener implementations
    public void onTimelineChanged(Timeline timeline, int reason) {
        // Implementation can be added if needed
    }

    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        // Implementation can be added if needed
    }

    public void onLoadingChanged(boolean isLoading) {
        setLoadingComplete(!isLoading);
        isLoadingNow = isLoading;
    }

    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_READY && playWhenReady && !isInProgress) {
            // Player is ready and playing
            isInProgress = true;
        } else if (playbackState == Player.STATE_ENDED) {
            // Playback ended
            if (mExoPlayer != null) {
                mExoPlayer.seekToDefaultPosition();
                mExoPlayer.setPlayWhenReady(false);
            }
            isInProgress = false;
        }
        
        if (playbackState == Player.STATE_BUFFERING) {
            isLoadingNow = true;
        } else if (playbackState == Player.STATE_READY) {
            isLoadingNow = false;
        }
    }

    public void onRepeatModeChanged(int repeatMode) {
        // Implementation can be added if needed
    }

    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        // Implementation can be added if needed
    }

    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "ExoPlayer error: " + error.getMessage());
        // Could implement fallback logic here
    }

    public void onPositionDiscontinuity(int reason) {
        // Implementation can be added if needed
    }

    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // Implementation can be added if needed
    }

    public void onSeekProcessed() {
        // Implementation can be added if needed
    }

    // Cleanup
    public void release() {
        if (mExoPlayer != null) {
            mExoPlayer.removeListener(this);
            mExoPlayer.release();
            mExoPlayer = null;
        }
    }
}