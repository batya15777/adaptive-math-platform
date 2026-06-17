package com.adaptive.server.service;

import com.adaptive.server.DTOs.UpdateProfileRequest;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.UserProfile;
import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;
import com.adaptive.server.repository.UserProfileRepository;
import com.adaptive.server.responses.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    // Keep in sync with the number of avatars in client/src/assets/avatars/
    private static final long MAX_PICTURE_ID = 1;

    private final UserProfileRepository profileRepository;

    public UserProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public UserProfileResponse getProfile(User user) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));
        return new UserProfileResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));

        if (request.getTheme() != null)    profile.setTheme(request.getTheme());
        if (request.getLanguage() != null) profile.setLanguage(request.getLanguage());
        if (request.getPictureId() != null) {
            long pid = request.getPictureId();
            if (pid < 0 || pid > MAX_PICTURE_ID)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "pictureId must be between 0 and " + MAX_PICTURE_ID);
            profile.setPictureId(pid);
        }

        profileRepository.save(profile);
        return new UserProfileResponse(profile);
    }

    // Creates and persists a profile with sensible defaults on first access.
    private UserProfile createDefaultProfile(User user) {
        UserProfile profile = new UserProfile(user, ProfileTheme.LIGHT, Language.HEBREW, 0);
        return profileRepository.save(profile);
    }
}
