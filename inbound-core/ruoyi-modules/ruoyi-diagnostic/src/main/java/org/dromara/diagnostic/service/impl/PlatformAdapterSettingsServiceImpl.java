package org.dromara.diagnostic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.diagnostic.domain.PlatformAdapter;
import org.dromara.diagnostic.domain.bo.PlatformAdapterSaveBo;
import org.dromara.diagnostic.domain.vo.PlatformAdapterAdminVo;
import org.dromara.diagnostic.mapper.PlatformAdapterMapper;
import org.dromara.diagnostic.service.IPlatformAdapterSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformAdapterSettingsServiceImpl implements IPlatformAdapterSettingsService {

    private static final String DEFAULT_VERSION = "1.0";

    private final PlatformAdapterMapper platformAdapterMapper;

    @Override
    public List<PlatformAdapterAdminVo> listAdapters(Long tenantId) {
        List<PlatformAdapter> rows = platformAdapterMapper.selectList(
            Wrappers.lambdaQuery(PlatformAdapter.class)
                .eq(PlatformAdapter::getTenantId, tenantId)
                .isNull(PlatformAdapter::getDeletedAt)
                .orderByAsc(PlatformAdapter::getPlatform)
        );
        return rows.stream().map(this::toSummaryVo).toList();
    }

    @Override
    public PlatformAdapterAdminVo getAdapter(Long tenantId, String platform) {
        PlatformAdapter row = findAdapterOrThrow(tenantId, platform);
        return toDetailVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformAdapterAdminVo saveAdapter(Long tenantId, String platform, PlatformAdapterSaveBo bo) {
        if (bo == null) {
            throw new ServiceException("请求体不能为空", HttpStatus.BAD_REQUEST);
        }
        String version = StringUtils.blankToDefault(bo.getVersion(), DEFAULT_VERSION);
        Map<String, Object> domSelectors = requireJsonObject(bo.getDomSelectorsJson(), "domSelectorsJson");
        Map<String, Object> apiPatterns = requireJsonObject(bo.getApiPatternsJson(), "apiPatternsJson");
        Map<String, Object> parseRules = requireJsonObject(bo.getParseRulesJson(), "parseRulesJson");
        boolean enabled = bo.getEnabled() == null || bo.getEnabled();

        PlatformAdapter existing = platformAdapterMapper.selectOne(
            Wrappers.lambdaQuery(PlatformAdapter.class)
                .eq(PlatformAdapter::getTenantId, tenantId)
                .eq(PlatformAdapter::getPlatform, platform)
                .eq(PlatformAdapter::getVersion, version)
                .isNull(PlatformAdapter::getDeletedAt)
                .last("LIMIT 1")
        );

        OffsetDateTime now = OffsetDateTime.now();
        if (existing == null) {
            PlatformAdapter created = new PlatformAdapter();
            created.setTenantId(tenantId);
            created.setPlatform(platform);
            created.setVersion(version);
            created.setDomSelectorsJson(domSelectors);
            created.setApiPatternsJson(apiPatterns);
            created.setParseRulesJson(parseRules);
            created.setEnabled(enabled);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            created.setCreatedBy(LoginHelper.getUserId());
            platformAdapterMapper.insert(created);
            return toDetailVo(created);
        }

        existing.setDomSelectorsJson(domSelectors);
        existing.setApiPatternsJson(apiPatterns);
        existing.setParseRulesJson(parseRules);
        existing.setEnabled(enabled);
        existing.setUpdatedAt(now);
        platformAdapterMapper.updateById(existing);
        return toDetailVo(existing);
    }

    private PlatformAdapter findAdapterOrThrow(Long tenantId, String platform) {
        if (StringUtils.isBlank(platform)) {
            throw new ServiceException("platform 不能为空", HttpStatus.BAD_REQUEST);
        }
        PlatformAdapter row = platformAdapterMapper.selectOne(
            Wrappers.lambdaQuery(PlatformAdapter.class)
                .eq(PlatformAdapter::getTenantId, tenantId)
                .eq(PlatformAdapter::getPlatform, platform)
                .isNull(PlatformAdapter::getDeletedAt)
                .orderByDesc(PlatformAdapter::getUpdatedAt)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new ServiceException("平台 Adapter 不存在: " + platform, HttpStatus.NOT_FOUND);
        }
        return row;
    }

    private static Map<String, Object> requireJsonObject(Map<String, Object> value, String field) {
        if (value == null || value.isEmpty()) {
            throw new ServiceException(field + " 不能为空", HttpStatus.BAD_REQUEST);
        }
        return new HashMap<>(value);
    }

    private PlatformAdapterAdminVo toSummaryVo(PlatformAdapter row) {
        PlatformAdapterAdminVo vo = new PlatformAdapterAdminVo();
        vo.setId(row.getId());
        vo.setPlatform(row.getPlatform());
        vo.setVersion(row.getVersion());
        vo.setEnabled(row.getEnabled());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private PlatformAdapterAdminVo toDetailVo(PlatformAdapter row) {
        PlatformAdapterAdminVo vo = toSummaryVo(row);
        vo.setDomSelectorsJson(row.getDomSelectorsJson());
        vo.setApiPatternsJson(row.getApiPatternsJson());
        vo.setParseRulesJson(row.getParseRulesJson());
        return vo;
    }
}
