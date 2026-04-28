package de.bbajor.pvs.api.v1.common;

import java.util.List;

public record SliceResponse<T>(
        List<T> items,
        boolean hasNext) {
}

