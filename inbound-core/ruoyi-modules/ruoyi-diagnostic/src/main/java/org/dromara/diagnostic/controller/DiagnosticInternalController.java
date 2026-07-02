package org.dromara.diagnostic.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.config.AiServiceProperties;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.diagnostic.domain.bo.ProbeCallbackBo;
import org.dromara.diagnostic.service.IDiagnosticRunService;
import org.dromara.diagnostic.service.IDiagnosticScheduleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GEO 诊断内网回调 — AI worker 写回 diagnostic_result。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/internal/diagnostics")
public class DiagnosticInternalController extends BaseController {

    private final IDiagnosticRunService diagnosticRunService;
    private final IDiagnosticScheduleService diagnosticScheduleService;
    private final AiServiceProperties aiServiceProperties;

    @PostMapping("/probe-callback")
    public R<Void> probeCallback(@Validated @RequestBody ProbeCallbackBo bo, HttpServletRequest request) {
        validateInternalToken(request);
        diagnosticRunService.handleProbeCallback(bo);
        return R.ok();
    }

    /** smoke / 内网手动触发定时诊断 */
    @PostMapping("/schedule-trigger")
    public R<Long> scheduleTrigger(
        @RequestParam Long projectId,
        @RequestParam(defaultValue = "true") boolean force,
        HttpServletRequest request
    ) {
        validateInternalToken(request);
        if (projectId == null) {
            throw new ServiceException("projectId 不能为空", HttpStatus.BAD_REQUEST);
        }
        return R.ok(diagnosticScheduleService.triggerForProject(projectId, force));
    }

    private void validateInternalToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (StringUtils.isBlank(auth) || !auth.startsWith("Bearer ")) {
            throw new ServiceException("未授权", HttpStatus.UNAUTHORIZED);
        }
        String token = auth.substring("Bearer ".length()).trim();
        if (!token.equals(aiServiceProperties.getInternalToken())) {
            throw new ServiceException("内网 Token 无效", HttpStatus.UNAUTHORIZED);
        }
    }
}
