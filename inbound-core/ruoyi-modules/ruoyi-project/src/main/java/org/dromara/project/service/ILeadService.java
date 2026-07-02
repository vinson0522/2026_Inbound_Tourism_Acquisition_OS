package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.LeadFollowupCreateBo;
import org.dromara.project.domain.bo.LeadQueryBo;
import org.dromara.project.domain.bo.LeadUpdateBo;
import org.dromara.project.domain.bo.PublicLeadEventBo;
import org.dromara.project.domain.bo.PublicLeadSubmitBo;
import org.dromara.project.domain.vo.LeadAiSuggestionVo;
import org.dromara.project.domain.vo.LeadDetailVo;
import org.dromara.project.domain.vo.LeadFollowupVo;
import org.dromara.project.domain.vo.LeadVo;
import org.dromara.project.domain.vo.PublicLeadEventVo;
import org.dromara.project.domain.vo.PublicLeadSubmitVo;

import java.util.List;

public interface ILeadService {

    PublicLeadSubmitVo submitPublic(PublicLeadSubmitBo bo, String turnstileToken);

    PublicLeadEventVo recordPublicEvent(PublicLeadEventBo bo);

    LeadAiSuggestionVo generateAiSuggestion(Long projectId, Long leadId);

    TableDataInfo<LeadVo> queryPageList(Long projectId, LeadQueryBo bo, PageQuery pageQuery);

    LeadDetailVo queryById(Long projectId, Long leadId);

    LeadDetailVo updateLead(Long projectId, Long leadId, LeadUpdateBo bo);

    List<LeadFollowupVo> listFollowups(Long projectId, Long leadId);

    LeadFollowupVo createFollowup(Long projectId, Long leadId, LeadFollowupCreateBo bo);
}
