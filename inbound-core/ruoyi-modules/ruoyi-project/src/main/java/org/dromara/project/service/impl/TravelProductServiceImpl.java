package org.dromara.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.project.domain.CustomerProject;
import org.dromara.project.domain.TravelProduct;
import org.dromara.project.domain.bo.TravelProductBo;
import org.dromara.project.domain.vo.TravelProductVo;
import org.dromara.project.mapper.CustomerProjectMapper;
import org.dromara.project.mapper.TravelProductMapper;
import org.dromara.project.service.ITravelProductService;
import org.dromara.common.tenant.helper.BusinessTenantHelper;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelProductServiceImpl implements ITravelProductService {

    private final TravelProductMapper travelProductMapper;
    private final CustomerProjectMapper customerProjectMapper;

    @Override
    public List<TravelProductVo> queryList(Long projectId) {
        assertProjectOwned(projectId);
        LambdaQueryWrapper<TravelProduct> lqw = Wrappers.lambdaQuery(TravelProduct.class)
            .eq(TravelProduct::getProjectId, projectId)
            .eq(TravelProduct::getTenantId, BusinessTenantHelper.getBusinessTenantId())
            .isNull(TravelProduct::getDeletedAt)
            .orderByDesc(TravelProduct::getCreatedAt);
        return travelProductMapper.selectVoList(lqw);
    }

    @Override
    public TravelProductVo queryById(Long projectId, Long productId) {
        return MapstructUtils.convert(getOwnedProductOrThrow(projectId, productId), TravelProductVo.class);
    }

    @Override
    public Long insertByBo(Long projectId, TravelProductBo bo) {
        assertProjectOwned(projectId);
        TravelProduct entity = MapstructUtils.convert(bo, TravelProduct.class);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(LoginHelper.getUserId());
        travelProductMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Boolean updateByBo(Long projectId, TravelProductBo bo) {
        getOwnedProductOrThrow(projectId, bo.getId());
        TravelProduct entity = MapstructUtils.convert(bo, TravelProduct.class);
        entity.setProjectId(projectId);
        entity.setTenantId(BusinessTenantHelper.getBusinessTenantId());
        entity.setUpdatedAt(OffsetDateTime.now());
        return travelProductMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteById(Long projectId, Long productId) {
        getOwnedProductOrThrow(projectId, productId);
        return travelProductMapper.update(
            null,
            Wrappers.lambdaUpdate(TravelProduct.class)
                .set(TravelProduct::getDeletedAt, OffsetDateTime.now())
                .set(TravelProduct::getUpdatedAt, OffsetDateTime.now())
                .eq(TravelProduct::getId, productId)
                .eq(TravelProduct::getProjectId, projectId)
                .eq(TravelProduct::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(TravelProduct::getDeletedAt)
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

    private TravelProduct getOwnedProductOrThrow(Long projectId, Long productId) {
        assertProjectOwned(projectId);
        TravelProduct entity = travelProductMapper.selectOne(
            Wrappers.lambdaQuery(TravelProduct.class)
                .eq(TravelProduct::getId, productId)
                .eq(TravelProduct::getProjectId, projectId)
                .eq(TravelProduct::getTenantId, BusinessTenantHelper.getBusinessTenantId())
                .isNull(TravelProduct::getDeletedAt)
        );
        if (entity == null) {
            throw new ServiceException("路线不存在");
        }
        return entity;
    }
}
