package com.adaptive.server.service.admin;

import com.adaptive.server.DTOs.AdminUserDto;
import com.adaptive.server.DTOs.PagedUsersResponse;
import com.adaptive.server.entity.User;
import com.adaptive.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PagedUsersResponse getUsers(int page, int size) {
        // Guard against abuse / bad input — never load everything at once.
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"));
        Page<User> result = userRepository.findAll(pageable);

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
}
