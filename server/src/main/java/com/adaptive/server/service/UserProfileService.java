package com.adaptive.server.service;

import com.adaptive.server.DTOs.UpdateProfileRequest;
import com.adaptive.server.entity.User;
import com.adaptive.server.entity.UserProfile;
import com.adaptive.server.entity.enums.Language;
import com.adaptive.server.entity.enums.ProfileTheme;
import com.adaptive.server.repository.UserProfileRepository;
import com.adaptive.server.repository.UserRepository;
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
    private final UserRepository userRepository;

    public UserProfileService(UserProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UserProfileResponse getProfile(User user) {
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));
        return new UserProfileResponse(profile, user);
    }

    /**
     * Select an avatar — and buy it first if it's a priced one the student doesn't own yet.
     * Star deduction happens HERE (server-side, authoritative): the price comes from the
     * server catalog, never the client. Free / already-owned avatars are just selected.
     */
    @Transactional
    public UserProfileResponse selectAvatar(User user, String avatarId) {
        AvatarCatalog.Avatar avatar = AvatarCatalog.get(avatarId);
        if (avatar == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown avatar: " + avatarId);

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> createDefaultProfile(user));

        boolean owned = avatar.price == 0 || user.getOwnedAvatarIds().contains(avatarId);
        if (!owned) {
            if (user.getTotalStars() < avatar.price)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stars");
            user.setTotalStars(user.getTotalStars() - avatar.price); // deduct (server-side)
            user.getOwnedAvatarIds().add(avatarId);
        }

        user.setSelectedAvatarId(avatarId);
        userRepository.save(user);

        // Real-image avatars also drive the profile pictureId (shown across the app).
        if (avatar.pictureId != null) {
            profile.setPictureId(avatar.pictureId);
            profileRepository.save(profile);
        }

        return new UserProfileResponse(profile, user);
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

        // Identity fields live on the User entity (theme/language/pictureId live on the
        // profile). Apply them here so a student can edit their own name/gender — a blank
        // name is ignored so it can't be wiped; gender accepts "" to mean "unset".
        boolean userChanged = false;
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
            userChanged = true;
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
            userChanged = true;
        }
        if (userChanged) userRepository.save(user);

        return new UserProfileResponse(profile, user);
    }

    // Creates and persists a profile with sensible defaults on first access.
    private UserProfile createDefaultProfile(User user) {
        UserProfile profile = new UserProfile(user, ProfileTheme.LIGHT, Language.HEBREW, 0);
        return profileRepository.save(profile);
    }
}
