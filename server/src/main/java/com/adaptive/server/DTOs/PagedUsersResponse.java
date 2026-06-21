package com.adaptive.server.DTOs;

import java.util.List;

/**
 * Paginated envelope for the admin user list.
 */
public class PagedUsersResponse {
    private final List<AdminUserDto> users;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PagedUsersResponse(List<AdminUserDto> users, int page, int size,
                              long totalElements, int totalPages) {
        this.users = users;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<AdminUserDto> getUsers() { return users; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
