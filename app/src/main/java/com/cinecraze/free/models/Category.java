package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Category {

    @SerializedName("MainCategory")
    private String mainCategory;

    @SerializedName("SubCategories")
    private List<String> subCategories;

    @SerializedName("Entries")
    private List<Entry> entries;

    public String getMainCategory() {
        return mainCategory;
    }

    public List<String> getSubCategories() {
        return subCategories;
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
