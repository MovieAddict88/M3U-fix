package com.cinecraze.free.models;

import com.google.gson.annotations.SerializedName;

public class Server {

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("license")
    private String license;

    @SerializedName("drm")
    private Boolean drm;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Boolean getDrm() {
        return drm;
    }

    public void setDrm(Boolean drm) {
        this.drm = drm;
    }

    /**
     * Check if this server has license information (DRM or non-DRM)
     */
    public boolean hasLicense() {
        return license != null && !license.trim().isEmpty();
    }

    /**
     * Check if this server requires DRM protection
     */
    public boolean isDrmProtected() {
        // If drm field is explicitly set, use that
        if (drm != null) {
            return drm;
        }
        // Fallback: assume DRM if license is present
        return hasLicense();
    }

    /**
     * Check if this server has non-DRM license (like authentication tokens)
     */
    public boolean hasNonDrmLicense() {
        return hasLicense() && !isDrmProtected();
    }
}