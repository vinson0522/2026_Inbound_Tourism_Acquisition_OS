package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.MaterialAssetQueryBo;
import org.dromara.project.domain.vo.MaterialAssetVo;
import org.dromara.project.domain.vo.MaterialBreakdownTriggerVo;
import org.dromara.project.domain.vo.VideoBreakdownVo;
import org.dromara.project.service.IMaterialAssetService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 爆款素材 API — EPIC-5 M1 FR-401~403
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
public class MaterialAssetController extends BaseController {

    private final IMaterialAssetService materialAssetService;

    @SaCheckLogin
    @GetMapping("/materials")
    public TableDataInfo<MaterialAssetVo> list(
        @NotNull @PathVariable Long projectId,
        MaterialAssetQueryBo bo,
        PageQuery pageQuery
    ) {
        return materialAssetService.queryPageList(projectId, bo, pageQuery);
    }

    @SaCheckLogin
    @Log(title = "爆款素材", businessType = BusinessType.INSERT)
    @PostMapping(value = "/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public R<Long> upload(
        @NotNull @PathVariable Long projectId,
        @RequestPart("file") MultipartFile file,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String copyrightStatus,
        @RequestParam(required = false) String source
    ) {
        return R.ok(materialAssetService.upload(projectId, file, type, copyrightStatus, source));
    }

    @SaCheckLogin
    @Log(title = "爆款拆解", businessType = BusinessType.OTHER)
    @PostMapping("/materials/{materialId}/breakdown")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public R<MaterialBreakdownTriggerVo> triggerBreakdown(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long materialId
    ) {
        return R.ok(materialAssetService.triggerBreakdown(projectId, materialId));
    }

    @SaCheckLogin
    @GetMapping("/breakdowns/{breakdownId}")
    public R<VideoBreakdownVo> getBreakdown(
        @NotNull @PathVariable Long projectId,
        @NotNull @PathVariable Long breakdownId
    ) {
        return R.ok(materialAssetService.queryBreakdown(projectId, breakdownId));
    }
}
