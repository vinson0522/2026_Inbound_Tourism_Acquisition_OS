package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.MaterialAssetQueryBo;
import org.dromara.project.domain.bo.MaterialBreakdownCallbackBo;
import org.dromara.project.domain.vo.MaterialAssetVo;
import org.dromara.project.domain.vo.MaterialBreakdownTriggerVo;
import org.dromara.project.domain.vo.VideoBreakdownVo;
import org.springframework.web.multipart.MultipartFile;

public interface IMaterialAssetService {

    TableDataInfo<MaterialAssetVo> queryPageList(Long projectId, MaterialAssetQueryBo bo, PageQuery pageQuery);

    Long upload(Long projectId, MultipartFile file, String type, String copyrightStatus, String source);

    MaterialBreakdownTriggerVo triggerBreakdown(Long projectId, Long materialId);

    VideoBreakdownVo queryBreakdown(Long projectId, Long breakdownId);

    void handleBreakdownCallback(MaterialBreakdownCallbackBo bo);
}
