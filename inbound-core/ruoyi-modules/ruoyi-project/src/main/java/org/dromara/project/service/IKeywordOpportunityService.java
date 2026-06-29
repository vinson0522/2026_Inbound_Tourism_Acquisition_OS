package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.KeywordGenerateBo;
import org.dromara.project.domain.bo.KeywordOpportunityBo;
import org.dromara.project.domain.vo.KeywordGenerateVo;
import org.dromara.project.domain.vo.KeywordOpportunityVo;

public interface IKeywordOpportunityService {

    TableDataInfo<KeywordOpportunityVo> queryPageList(Long projectId, KeywordOpportunityBo bo, PageQuery pageQuery);

    Long insertByBo(Long projectId, KeywordOpportunityBo bo);

    Boolean deleteById(Long projectId, Long keywordId);

    KeywordGenerateVo generateKeywords(Long projectId, KeywordGenerateBo bo);
}
