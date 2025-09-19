package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Playlist {

    @SerializedName("Categories")
    private List<Category> categories;

    public List<Category> getCategories() {
        return categories;
    }
}
