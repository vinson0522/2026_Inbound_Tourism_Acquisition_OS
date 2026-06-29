package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.KnowledgeAssetBo;
import org.dromara.project.domain.vo.KnowledgeAssetVo;

public interface IKnowledgeAssetService {

    TableDataInfo<KnowledgeAssetVo> queryPageList(Long projectId, KnowledgeAssetBo bo, PageQuery pageQuery);

    KnowledgeAssetVo queryById(Long projectId, Long assetId);

    Long insertByBo(Long projectId, KnowledgeAssetBo bo);

    Boolean updateByBo(Long projectId, Long assetId, KnowledgeAssetBo bo);

    Boolean deleteById(Long projectId, Long assetId);

    Boolean triggerReindex(Long projectId, Long assetId);
}
