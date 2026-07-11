package br.com.oficina.os.interfaces.presenters.view_model;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public static <T> PageResponse<T> of(List<T> allItems, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 100);
        int from = Math.min(safePage * safeSize, allItems.size());
        int to = Math.min(from + safeSize, allItems.size());
        int totalPages = allItems.isEmpty() ? 0 : (int) Math.ceil((double) allItems.size() / safeSize);
        return new PageResponse<>(
                allItems.subList(from, to),
                safePage,
                safeSize,
                allItems.size(),
                totalPages);
    }
}
