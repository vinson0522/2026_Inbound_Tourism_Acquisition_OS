package org.dromara.project.support;

import org.dromara.common.core.exception.ServiceException;

import java.util.Set;

/**
 * EPIC-7 M2 lead_status 状态机 — ADR-20
 */
public final class LeadStatusTransition {

    private static final Set<String> TERMINAL = Set.of("WON", "LOST");

    private LeadStatusTransition() {
    }

    public static boolean isTerminal(String status) {
        return status != null && TERMINAL.contains(status);
    }

    public static void validateTransition(String from, String to) {
        if (from == null || to == null) {
            throw new ServiceException("状态无效");
        }
        if (from.equals(to)) {
            return;
        }
        if (isTerminal(from)) {
            throw new ServiceException("终态线索不可变更状态");
        }
        boolean allowed = switch (from) {
            case "NEW" -> "FOLLOWING".equals(to) || "LOST".equals(to);
            case "FOLLOWING" -> "QUOTED".equals(to) || "LOST".equals(to);
            case "QUOTED" -> "WON".equals(to) || "LOST".equals(to);
            default -> false;
        };
        if (!allowed) {
            throw new ServiceException("不允许的状态流转: " + from + " → " + to);
        }
    }
}
