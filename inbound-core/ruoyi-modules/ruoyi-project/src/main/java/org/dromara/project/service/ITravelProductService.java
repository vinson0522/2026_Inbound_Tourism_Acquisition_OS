package org.dromara.project.service;

import org.dromara.project.domain.bo.TravelProductBo;
import org.dromara.project.domain.vo.TravelProductVo;

import java.util.List;

public interface ITravelProductService {

    List<TravelProductVo> queryList(Long projectId);

    TravelProductVo queryById(Long projectId, Long productId);

    Long insertByBo(Long projectId, TravelProductBo bo);

    Boolean updateByBo(Long projectId, TravelProductBo bo);

    Boolean deleteById(Long projectId, Long productId);
}
