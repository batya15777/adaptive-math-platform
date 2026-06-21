package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminUserDto;
import com.adaptive.server.DTOs.PagedUsersResponse;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_ROLES = Set.of("STUDENT", "ADMIN");
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "BLOCKED", "DELETED");

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PagedUsersResponse getUsers(int page, int size, String search, String role, String status) {
        // Guard against abuse / bad input — never load everything at once.
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // Blank search → no text filter. Numeric search also matches an exact id.
        String q = (search == null || search.trim().isEmpty()) ? null : search.trim();
        Long idQ = parseIdOrNull(q);

        // Blank role → All. Any other value must be a valid role, else 400.
        String roleFilter = (role == null || role.trim().isEmpty()) ? null : role.trim().toUpperCase();
        if (roleFilter != null && !VALID_ROLES.contains(roleFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role filter. Use STUDENT or ADMIN.");
        }

        List<String> statuses = resolveStatuses(status);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<User> result = userRepository.searchUsers(q, idQ, roleFilter, statuses, pageable);

        List<AdminUserDto> users = result.getContent().stream()
                .map(AdminUserDto::new)
                .collect(Collectors.toList());

        return new PagedUsersResponse(
                users,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public AdminUserDto changeRole(Long currentAdminId, Long targetId, String rawRole) {
        String role = (rawRole == null) ? null : rawRole.trim().toUpperCase();
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role. Use STUDENT or ADMIN.");
        }
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (role.equals(user.getRole())) {
            return new AdminUserDto(user); // no-op: already that role
        }

        boolean demotingFromAdmin = "ADMIN".equals(user.getRole()) && "STUDENT".equals(role);
        if (demotingFromAdmin && targetId.equals(currentAdminId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own admin role.");
        }
        if (demotingFromAdmin && userRepository.countByRole("ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot remove the last admin.");
        }

        user.setRole(role);
        return new AdminUserDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto setStatus(Long currentAdminId, Long targetId, String rawStatus) {
        String status = (rawStatus == null) ? null : rawStatus.trim().toUpperCase();
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status. Use ACTIVE or BLOCKED.");
        }
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (status.equals(user.getAccountStatus())) {
            return new AdminUserDto(user); // no-op: already in that status
        }

        // Any transition away from ACTIVE (BLOCKED or DELETED) removes the user's access.
        boolean deactivating = !"ACTIVE".equals(status);
        if (deactivating) {
            if (targetId.equals(currentAdminId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot block or delete yourself.");
            }
            if ("ADMIN".equals(user.getRole())
                    && userRepository.countByRoleAndAccountStatus("ADMIN", "ACTIVE") <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot block or delete the last active admin.");
            }
        }

        user.setAccountStatus(status);
        return new AdminUserDto(userRepository.save(user));
    }

    // Resolves the status filter to the set of account statuses to include.
    // Blank → ACTIVE + BLOCKED (hide DELETED by default); ALL → everything.
    private List<String> resolveStatuses(String status) {
        String s = (status == null || status.trim().isEmpty()) ? null : status.trim().toUpperCase();
        if (s == null) {
            return List.of("ACTIVE", "BLOCKED");
        }
        if ("ALL".equals(s)) {
            return List.of("ACTIVE", "BLOCKED", "DELETED");
        }
        if (VALID_STATUSES.contains(s)) {
            return List.of(s);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid status filter. Use ACTIVE, BLOCKED, DELETED or ALL.");
    }

    // Returns the id when the search text is purely numeric, else null.
    private Long parseIdOrNull(String q) {
        if (q == null) {
            return null;
        }
        try {
            return Long.parseLong(q);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
