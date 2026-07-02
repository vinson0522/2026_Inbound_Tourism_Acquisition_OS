package org.dromara.project.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.aiclient.config.AiServiceProperties;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.MaterialBreakdownCallbackBo;
import org.dromara.project.service.IMaterialAssetService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 爆款拆解内网回调 — Python worker 写回 video_breakdown
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/internal/materials")
public class MaterialInternalController extends BaseController {

    private final IMaterialAssetService materialAssetService;
    private final AiServiceProperties aiServiceProperties;

    @PostMapping("/breakdown-callback")
    public R<Void> breakdownCallback(@Validated @RequestBody MaterialBreakdownCallbackBo bo, HttpServletRequest request) {
        validateInternalToken(request);
        materialAssetService.handleBreakdownCallback(bo);
        return R.ok();
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
