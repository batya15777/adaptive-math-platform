package com.adaptive.server.controllers;

import com.adaptive.server.DTOs.SelectAvatarRequest;
import com.adaptive.server.DTOs.UpdateProfileRequest;
import com.adaptive.server.entity.SessionToken;
import com.adaptive.server.entity.User;
import com.adaptive.server.responses.ProfileOptionsResponse;
import com.adaptive.server.responses.UserProfileResponse;
import com.adaptive.server.service.SessionValidationService;
import com.adaptive.server.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SessionValidationService sessionValidationService;

    public UserProfileController(UserProfileService userProfileService,
                                 SessionValidationService sessionValidationService) {
        this.userProfileService    = userProfileService;
        this.sessionValidationService = sessionValidationService;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            @CookieValue(value = "session_token", required = false) String sessionToken) {
        User user = resolveUser(sessionToken);
        return ResponseEntity.ok(userProfileService.getProfile(user));
    }

    // Static metadata (theme/language labels) — no session needed.
    @GetMapping("/options")
    public ResponseEntity<ProfileOptionsResponse> getOptions() {
        return ResponseEntity.ok(new ProfileOptionsResponse());
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody UpdateProfileRequest request) {
        User user = resolveUser(sessionToken);
        return ResponseEntity.ok(userProfileService.updateProfile(user, request));
    }

    // Select an avatar (buying it first if it's priced + not yet owned). Star deduction
    // is server-side — the price is read from the server catalog, never the client.
    @PostMapping("/avatar/select")
    public ResponseEntity<UserProfileResponse> selectAvatar(
            @CookieValue(value = "session_token", required = false) String sessionToken,
            @RequestBody SelectAvatarRequest request) {
        User user = resolveUser(sessionToken);
        return ResponseEntity.ok(userProfileService.selectAvatar(user, request.getAvatarId()));
    }

    private User resolveUser(String sessionToken) {
        SessionToken token = sessionValidationService.validateAndGetUser(sessionToken);
        return token.getUser();
    }
}
