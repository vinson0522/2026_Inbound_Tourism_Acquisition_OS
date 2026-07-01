package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.LeadQueryBo;
import org.dromara.project.domain.bo.PublicLeadSubmitBo;
import org.dromara.project.domain.vo.LeadDetailVo;
import org.dromara.project.domain.vo.LeadVo;
import org.dromara.project.domain.vo.PublicLeadSubmitVo;

public interface ILeadService {

    PublicLeadSubmitVo submitPublic(PublicLeadSubmitBo bo, String turnstileToken);

    TableDataInfo<LeadVo> queryPageList(Long projectId, LeadQueryBo bo, PageQuery pageQuery);

    LeadDetailVo queryById(Long projectId, Long leadId);
}
