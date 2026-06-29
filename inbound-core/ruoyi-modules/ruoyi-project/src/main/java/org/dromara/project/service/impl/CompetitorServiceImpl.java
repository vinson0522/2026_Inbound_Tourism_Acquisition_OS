package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.Competitor;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.bo.CompetitorBo;
import org.dromara.project.domain.vo.CompetitorVo;
import org.dromara.project.mapper.CompetitorMapper;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.service.ICompetitorService;
import org.dromara.project.support.BusinessTenantHelper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompetitorServiceImpl implements ICompetitorService {

    private final CompetitorMapper competitorMapper;
    private final CustomerProjectMapper customerProjectMapper;

    @Override
    public List<CompetitorVo> queryList(Long projectId) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<Competitor> lqw = Wrappers.lambdaQuery(Competitor.class)
            .eq(Competitor::getProjectId, projectId)
            .eq(Competitor::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(Competitor::getDeletedAt)
            .orderByDesc(Competitor::getCreatedAt);
        return competitorMapper.selectVoList(lqw);
    }

    @Override
    public CompetitorVo queryById(Long projectId, Long competitorId) {
        return MapstructUtils.convert(getOwnedCompetitorOrThrow(projectId, competitorId), CompetitorVo.class);
    }

    @Override
    public Long insertByBo(Long projectId, CompetitorBo bo) {
        assertProjectOwned(projectId);
        Competitor entity = MapstructUtils.convert(bo, Competitor.class);
        if (entity.getSocialLinks() == null) {
            entity.setSocialLinks(Collections.emptyMap());
        }
        OffsetDateTime now = OffsetDateTime.now();
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        competitorMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateByBo(Long projectId, CompetitorBo bo) {
        getOwnedCompetitorOrThrow(projectId, bo.getId());
        Competitor entity = MapstructUtils.convert(bo, Competitor.class);
        if (entity.getSocialLinks() == null) {
            entity.setSocialLinks(Collections.emptyMap());
        }
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setUpdatedAt(OffsetDateTime.now());
        return competitorMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteById(Long projectId, Long competitorId) {
        getOwnedCompetitorOrThrow(projectId, competitorId);
        return competitorMapper.update(
            null,
            Wrappers.lambdaUpdate(Competitor.class)
                .set(Competitor::getDeletedAt, OffsetDateTime.now())
                .set(Competitor::getUpdatedAt, OffsetDateTime.now())
                .eq(Competitor::getId, competitorId)
                .eq(Competitor::getProjectId, projectId)
                .eq(Competitor::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(Competitor::getDeletedAt)
        ) > 0;
    }

    private void assertProjectOwned(Long projectId) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        CustomerProject project = customerProjectMapper.selectOne(
            Wrappers.lambdaQuery(CustomerProject.class)
                .eq(CustomerProject::getId, projectId)
                .eq(CustomerProject::getTenantId, tenantId)
                .isNull(CustomerProject::getDeletedAt)
        );
        if (project == null) {
            throw new ServiceException("项目不存在或无权访问");
        }
    }

    private Competitor getOwnedCompetitorOrThrow(Long projectId, Long competitorId) {
        assertProjectOwned(projectId);
        Competitor entity = competitorMapper.selectOne(
            Wrappers.lambdaQuery(Competitor.class)
                .eq(Competitor::getId, competitorId)
                .eq(Competitor::getProjectId, projectId)
                .eq(Competitor::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(Competitor::getDeletedAt)
        );
        if (entity == null) {
            throw new ServiceException("竞品不存在");
        }
        return entity;
    }
}
