package org.dromara.project.service;

import org.dromara.project.domain.bo.CompetitorBo;
import org.dromara.project.domain.vo.CompetitorVo;

import java.util.List;

public interface ICompetitorService {

    List<CompetitorVo> queryList(Long projectId);

    CompetitorVo queryById(Long projectId, Long competitorId);

    Long insertByBo(Long projectId, CompetitorBo bo);

    Boolean updateByBo(Long projectId, CompetitorBo bo);

    Boolean deleteById(Long projectId, Long competitorId);
}
