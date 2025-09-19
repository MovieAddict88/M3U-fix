package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PlaylistsVersion {

    @SerializedName("version")
    private int version;

    @SerializedName("playlists")
    private List<String> playlists;

    public int getVersion() {
        return version;
    }

    public List<String> getPlaylists() {
        return playlists;
    }
}
