package org.dromara.diagnostic.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.diagnostic.config.ProbeProperties;
import org.dromara.diagnostic.domain.bo.ProbeExtensionResultBo;
import org.dromara.diagnostic.domain.bo.ProbeNodeRegisterBo;
import org.dromara.diagnostic.domain.vo.PlatformAdapterVo;
import org.dromara.diagnostic.domain.vo.ProbeNodeVo;
import org.dromara.diagnostic.domain.vo.ProbePollTaskVo;
import org.dromara.diagnostic.service.IProbeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * EPIC-11 M1 — 浏览器探针扩展 API（FR-112~114）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/probe")
public class ProbeController extends BaseController {

    private final IProbeService probeService;

    @SaIgnore
    @PostMapping("/nodes/register")
    public R<Long> register(@Validated @RequestBody ProbeNodeRegisterBo bo, HttpServletRequest request) {
        String nodeKey = request.getHeader(ProbeProperties.NODE_KEY_HEADER);
        return R.ok(probeService.registerNode(bo, nodeKey));
    }

    @SaCheckLogin
    @GetMapping("/nodes")
    public R<List<ProbeNodeVo>> listNodes() {
        return R.ok(probeService.listNodesForCurrentTenant());
    }

    @SaIgnore
    @GetMapping("/tasks/poll")
    public R<ProbePollTaskVo> poll(
        @RequestParam String platform,
        HttpServletRequest request
    ) {
        String nodeKey = request.getHeader(ProbeProperties.NODE_KEY_HEADER);
        ProbePollTaskVo task = probeService.pollTask(platform, nodeKey);
        return R.ok(task);
    }

    @SaIgnore
    @PostMapping("/tasks/{probeTaskId}/result")
    public R<Void> submitResult(
        @NotNull @PathVariable Long probeTaskId,
        @Validated @RequestBody ProbeExtensionResultBo bo,
        HttpServletRequest request
    ) {
        String nodeKey = request.getHeader(ProbeProperties.NODE_KEY_HEADER);
        probeService.submitResult(probeTaskId, bo, nodeKey);
        return R.ok();
    }

    @SaIgnore
    @GetMapping("/adapters")
    public R<List<PlatformAdapterVo>> adapters(HttpServletRequest request) {
        String nodeKey = request.getHeader(ProbeProperties.NODE_KEY_HEADER);
        return R.ok(probeService.listAdapters(nodeKey));
    }
}
