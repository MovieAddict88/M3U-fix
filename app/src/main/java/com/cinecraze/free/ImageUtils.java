package com.cinecraze.free;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.cinecraze.free.R;

public class ImageUtils {
    
    /**
     * Load image with uniform scaling for poster/movie images (square format)
     */
    public static void loadPosterImage(Context context, String imageUrl, ImageView imageView) {
        RequestOptions requestOptions = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .fallback(R.drawable.image_placeholder);
        
        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView);
    }
    
    /**
     * Load image with center crop for carousel/banner images
     */
    public static void loadBannerImage(Context context, String imageUrl, ImageView imageView) {
        RequestOptions requestOptions = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .fallback(R.drawable.image_placeholder);
        
        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView);
    }
    
    /**
     * Load thumbnail images for episodes/seasons with center crop scaling (square format)
     */
    public static void loadThumbnailImage(Context context, String imageUrl, ImageView imageView) {
        RequestOptions requestOptions = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .fallback(R.drawable.image_placeholder);
        
        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView);
    }
    
    /**
     * Load image with specific dimensions for consistent sizing
     */
    public static void loadImageWithSize(Context context, String imageUrl, ImageView imageView, int width, int height) {
        RequestOptions requestOptions = new RequestOptions()
                .override(width, height)
                .centerInside()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder)
                .fallback(R.drawable.image_placeholder);
        
        Glide.with(context)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView);
    }
}