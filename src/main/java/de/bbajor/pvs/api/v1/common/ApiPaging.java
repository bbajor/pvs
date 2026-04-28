package de.bbajor.pvs.api.v1.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class ApiPaging {

    private static final int DEFAULT_PAGE_SIZE = 25;
    private static final int MAX_PAGE_SIZE = 200;

    private ApiPaging() {
    }

    public static Pageable page(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        return PageRequest.of(safePage, safeSize);
    }

    public static Pageable defaultPage(int page) {
        return page(page, DEFAULT_PAGE_SIZE);
    }
}

