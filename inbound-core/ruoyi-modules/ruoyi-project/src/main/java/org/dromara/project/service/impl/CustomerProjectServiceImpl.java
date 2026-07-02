package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.billing.QuotaType;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.bo.CustomerProjectBo;
import org.dromara.project.domain.vo.CustomerProjectVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.service.ICustomerProjectService;
import org.dromara.project.service.IQuotaService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerProjectServiceImpl implements ICustomerProjectService {

    private final CustomerProjectMapper baseMapper;
    private final IQuotaService quotaService;

    @Override
    public TableDataInfo<CustomerProjectVo> queryPageList(CustomerProjectBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CustomerProject> lqw = buildQueryWrapper(bo);
        Page<CustomerProjectVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<CustomerProjectVo> queryList(CustomerProjectBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    @Override
    public CustomerProjectVo queryById(Long id) {
        CustomerProject entity = getOwnedProjectOrThrow(id);
        return MapstructUtils.convert(entity, CustomerProjectVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(CustomerProjectBo bo) {
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        quotaService.checkAndConsume(tenantId, QuotaType.PROJECTS, 1);
        CustomerProject entity = MapstructUtils.convert(bo, CustomerProject.class);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setTenantId(tenantId);
        entity.setIndustry(StringUtils.blankToDefault(entity.getIndustry(), "inbound_tourism"));
        entity.setStatus(StringUtils.blankToDefault(entity.getStatus(), "ACTIVE"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateByBo(CustomerProjectBo bo) {
        getOwnedProjectOrThrow(bo.getId());
        CustomerProject entity = MapstructUtils.convert(bo, CustomerProject.class);
        entity.setUpdatedAt(OffsetDateTime.now());
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        return baseMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteById(Long id) {
        getOwnedProjectOrThrow(id);
        return baseMapper.update(
            null,
            Wrappers.lambdaUpdate(CustomerProject.class)
                .set(CustomerProject::getDeletedAt, OffsetDateTime.now())
                .set(CustomerProject::getUpdatedAt, OffsetDateTime.now())
                .eq(CustomerProject::getId, id)
                .eq(CustomerProject::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(CustomerProject::getDeletedAt)
        ) > 0;
    }

    private CustomerProject getOwnedProjectOrThrow(Long id) {
        CustomerProject entity = baseMapper.selectById(id);
        if (entity == null || entity.getDeletedAt() != null) {
            throw new ServiceException("项目不存在", HttpStatus.NOT_FOUND);
        }
        Long tenantId = BusinessTenantHelper.getBusinessTenantId();
        if (!tenantId.equals(entity.getTenantId())) {
            throw new ServiceException("无权访问该项目", HttpStatus.FORBIDDEN);
        }
        return entity;
    }

    private LambdaQueryWrapper<CustomerProject> buildQueryWrapper(CustomerProjectBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<CustomerProject> lqw = Wrappers.lambdaQuery();
        lqw.eq(CustomerProject::getTenantId, BusinessTenantHelper.getBusinessTenantId());
        lqw.isNull(CustomerProject::getDeletedAt);
        lqw.like(StringUtils.isNotBlank(bo.getName()), CustomerProject::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getBrandName()), CustomerProject::getBrandName, bo.getBrandName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), CustomerProject::getStatus, bo.getStatus());
        if (StringUtils.isNotBlank(bo.getMarket())) {
            lqw.apply("target_markets_json @> {0}::jsonb", "[\"" + bo.getMarket() + "\"]");
        }
        lqw.between(
            params.get("beginCreateTime") != null && params.get("endCreateTime") != null,
            CustomerProject::getCreatedAt,
            params.get("beginCreateTime"),
            params.get("endCreateTime")
        );
        lqw.orderByDesc(CustomerProject::getCreatedAt);
        return lqw;
    }
}
