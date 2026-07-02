package org.dromara.project.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.config.AiServiceProperties;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.vo.SubscriptionVo;
import org.dromara.project.service.ISubscriptionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 套餐内网调试 — smoke 手动触发周期重置
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/internal/billing")
public class BillingInternalController extends BaseController {

    private final ISubscriptionService subscriptionService;
    private final AiServiceProperties aiServiceProperties;

    @PostMapping("/period-reset")
    public R<SubscriptionVo> periodReset(@RequestParam Long tenantId, HttpServletRequest request) {
        validateInternalToken(request);
        if (tenantId == null) {
            throw new ServiceException("tenantId 不能为空", HttpStatus.BAD_REQUEST);
        }
        return R.ok(subscriptionService.resetPeriod(tenantId));
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
