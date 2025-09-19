package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Season {

    @SerializedName("Season")
    private int season;

    @SerializedName("SeasonPoster")
    private String seasonPoster;

    @SerializedName("Episodes")
    private List<Episode> episodes;

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public String getSeasonPoster() {
        return seasonPoster;
    }

    public void setSeasonPoster(String seasonPoster) {
        this.seasonPoster = seasonPoster;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }
}
