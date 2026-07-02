package org.dromara.diagnostic.service;

import org.dromara.diagnostic.domain.bo.ProbeExtensionResultBo;
import org.dromara.diagnostic.domain.bo.ProbeNodeRegisterBo;
import org.dromara.diagnostic.domain.vo.PlatformAdapterVo;
import org.dromara.diagnostic.domain.vo.ProbeNodeVo;
import org.dromara.diagnostic.domain.vo.ProbePollTaskVo;

import java.util.List;

public interface IProbeService {

    Long registerNode(ProbeNodeRegisterBo bo, String nodeKeyHeader);

    ProbePollTaskVo pollTask(String platform, String nodeKeyHeader);

    void submitResult(Long probeTaskId, ProbeExtensionResultBo bo, String nodeKeyHeader);

    List<PlatformAdapterVo> listAdapters(String nodeKeyHeader);

    List<ProbeNodeVo> listNodesForCurrentTenant();
}
