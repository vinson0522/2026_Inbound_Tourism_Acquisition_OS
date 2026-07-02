package org.dromara.diagnostic.service;

import org.dromara.diagnostic.domain.bo.PlatformAdapterSaveBo;
import org.dromara.diagnostic.domain.vo.PlatformAdapterAdminVo;

import java.util.List;

public interface IPlatformAdapterSettingsService {

    List<PlatformAdapterAdminVo> listAdapters(Long tenantId);

    PlatformAdapterAdminVo getAdapter(Long tenantId, String platform);

    PlatformAdapterAdminVo saveAdapter(Long tenantId, String platform, PlatformAdapterSaveBo bo);
}
