package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Entry model class for movie/show entries
 * 
 * Note: Rating and Year fields can contain mixed data types:
 * - Rating: can be float (8.2), int (8), or String ("TV-Y7")
 * - Year: can be int (2021) or String
 * The getter methods handle type conversion safely.
 */
public class Entry {

    @SerializedName("Title")
    private String title;

    @SerializedName("SubCategory")
    private String subCategory;

    @SerializedName("MainCategory")
    private String mainCategory;

    @SerializedName("Country")
    private String country;

    @SerializedName("Description")
    private String description;

    @SerializedName("Poster")
    private String poster;

    @SerializedName("Thumbnail")
    private String thumbnail;

    @SerializedName("Rating")
    private Object rating; // Can be float, int, or String

    @SerializedName("Duration")
    private String duration;

    @SerializedName("Year")
    private Object year; // Can be int or String

    @SerializedName("Servers")
    private List<Server> servers;

    @SerializedName("Seasons")
    private List<Season> seasons;

    @SerializedName("Related")
    private List<Entry> related;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public float getRating() {
        if (rating instanceof Number) {
            return ((Number) rating).floatValue();
        } else if (rating instanceof String) {
            try {
                return Float.parseFloat((String) rating);
            } catch (NumberFormatException e) {
                return 0.0f; // Default to 0 if string cannot be parsed
            }
        }
        return 0.0f; // Default value
    }

    public String getRatingString() {
        if (rating instanceof String) {
            return (String) rating;
        } else if (rating instanceof Number) {
            return String.valueOf(rating);
        }
        return "0"; // Default value
    }

    public void setRating(Object rating) {
        this.rating = rating;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getYear() {
        if (year instanceof Number) {
            return ((Number) year).intValue();
        } else if (year instanceof String) {
            try {
                return Integer.parseInt((String) year);
            } catch (NumberFormatException e) {
                return 0; // Default to 0 if string cannot be parsed
            }
        }
        return 0; // Default value
    }

    public String getYearString() {
        if (year instanceof String) {
            return (String) year;
        } else if (year instanceof Number) {
            return String.valueOf(year);
        }
        return "0"; // Default value
    }

    public void setYear(Object year) {
        this.year = year;
    }

    public List<Server> getServers() {
        return servers;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    public List<Season> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<Season> seasons) {
        this.seasons = seasons;
    }

    public List<Entry> getRelated() {
        return related;
    }

    public void setRelated(List<Entry> related) {
        this.related = related;
    }

    public String getImageUrl() {
        return poster != null ? poster : thumbnail;
    }

    public int getId() {
        // Generate a simple hash-based ID from title and year
        return (title + getYearString()).hashCode();
    }
}
