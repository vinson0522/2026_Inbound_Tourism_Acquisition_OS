package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.LandingGenerateBo;
import org.dromara.project.domain.bo.LandingPageBo;
import org.dromara.project.domain.vo.LandingGenerateVo;
import org.dromara.project.domain.vo.LandingPageDetailVo;
import org.dromara.project.domain.vo.LandingPageVo;
import org.dromara.project.domain.vo.LandingPublishVo;
import org.dromara.project.domain.vo.PublicLandingPageVo;

public interface ILandingPageService {

    TableDataInfo<LandingPageVo> queryPageList(Long projectId, LandingPageBo bo, PageQuery pageQuery);

    LandingPageDetailVo queryById(Long projectId, Long pageId);

    Long insertByBo(Long projectId, LandingPageBo bo);

    Boolean deleteById(Long projectId, Long pageId);

    LandingGenerateVo generate(Long projectId, Long pageId, LandingGenerateBo bo);

    LandingPublishVo publish(Long projectId, Long pageId);

    LandingPublishVo unpublish(Long projectId, Long pageId);

    PublicLandingPageVo queryPublicPublished(Long projectId, String slug);
}
