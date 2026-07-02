package org.dromara.project.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.project.domain.VideoBreakdown;

import java.util.List;
import java.util.Map;

/**
 * 爆款拆解状态推导 — ADR-20260709-24
 */
public final class MaterialBreakdownSupport {

    public static final String STATUS_NONE = "NONE";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    private MaterialBreakdownSupport() {
    }

    public static String resolveStatus(VideoBreakdown breakdown) {
        if (breakdown == null) {
            return STATUS_NONE;
        }
        Map<String, Object> dimensions = breakdown.getDimensionsJson();
        if (dimensions != null && "FAILED".equals(String.valueOf(dimensions.get("_status")))) {
            return STATUS_FAILED;
        }
        if (isDimensionsReady(dimensions)) {
            return STATUS_SUCCESS;
        }
        return STATUS_PROCESSING;
    }

    public static boolean isDimensionsReady(Map<String, Object> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return false;
        }
        if ("FAILED".equals(String.valueOf(dimensions.get("_status")))) {
            return false;
        }
        return dimensions.containsKey("theme") || dimensions.containsKey("hook");
    }

    public static boolean readNeedsHumanReview(Map<String, Object> dimensions) {
        if (dimensions == null) {
            return false;
        }
        Object flag = dimensions.get("_needsHumanReview");
        if (flag instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(flag));
    }

    public static int frameCount(List<Map<String, Object>> frames) {
        return frames == null ? 0 : frames.size();
    }

    public static String extractFileName(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String path = url;
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    public static String inferMaterialType(String filename, String contentType) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm")
            || (contentType != null && contentType.startsWith("video/"))) {
            return "VIDEO";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
            || lower.endsWith(".gif") || (contentType != null && contentType.startsWith("image/"))) {
            return "IMAGE";
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || (contentType != null && contentType.startsWith("audio/"))) {
            return "AUDIO";
        }
        return "OTHER";
    }
}
