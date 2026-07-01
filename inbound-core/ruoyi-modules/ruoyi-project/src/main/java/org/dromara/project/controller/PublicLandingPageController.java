package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.vo.PublicLandingPageVo;
import org.dromara.project.service.ILandingPageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公网落地页 — EPIC-6 M2 FR-507
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/public/landing-pages")
public class PublicLandingPageController extends BaseController {

    private final ILandingPageService landingPageService;

    @GetMapping("/{slug}")
    public R<PublicLandingPageVo> getPublished(
        @NotBlank @PathVariable String slug,
        @NotNull @RequestParam Long projectId
    ) {
        return R.ok(landingPageService.queryPublicPublished(projectId, slug));
    }
}
