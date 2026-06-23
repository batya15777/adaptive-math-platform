package com.adaptive.server.DTOs;

// Body for POST /profile/avatar/select — the catalog id of the avatar to select/buy.
public class SelectAvatarRequest {

    private String avatarId;

    public SelectAvatarRequest() {}

    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
}
