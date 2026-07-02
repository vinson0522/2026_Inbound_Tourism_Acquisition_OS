package org.dromara.diagnostic.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.diagnostic.service.IDiagnosticScheduleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-109 定时 GEO 诊断 — 每小时扫描 due 计划并触发 createRun。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiagnosticScheduleJob {

    private final IDiagnosticScheduleService diagnosticScheduleService;

    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyDueCheck() {
        log.debug("diagnostic_schedule_job tick");
        diagnosticScheduleService.processDueSchedules();
    }
}
