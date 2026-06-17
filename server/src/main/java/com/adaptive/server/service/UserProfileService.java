package com.adaptive.server.service;

import com.adaptive.server.DTOs.UpdateProfileRequest;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.UserProfile;
import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;
import com.adaptive.server.repository.UserProfileRepository;
import com.adaptive.server.responses.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;

    public UserProfileService(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));
        return new UserProfileResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));

        if (request.getTheme() != null)     profile.setTheme(request.getTheme());
        if (request.getLanguage() != null)  profile.setLanguage(request.getLanguage());
        if (request.getPictureId() != null) profile.setPictureId(request.getPictureId());

        profileRepository.save(profile);
        return new UserProfileResponse(profile);
    }

    // Creates and persists a profile with sensible defaults on first access.
    private UserProfile createDefaultProfile(User user) {
        UserProfile profile = new UserProfile(user, ProfileTheme.LIGHT, Language.HEBREW, 0);
        return profileRepository.save(profile);
    }
}
