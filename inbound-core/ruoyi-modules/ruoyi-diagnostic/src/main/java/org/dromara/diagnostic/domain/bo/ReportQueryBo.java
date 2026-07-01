package org.dromara.diagnostic.domain.bo;

import lombok.Data;

/**
 * 报告列表筛选 — EPIC-8 FR-701
 */
@Data
public class ReportQueryBo {

    /** DIAGNOSTIC | WEEKLY | MONTHLY | CUSTOM */
    private String type;

    /** ISO 周如 2026-W26，或诊断 runId */
    private String period;
}
