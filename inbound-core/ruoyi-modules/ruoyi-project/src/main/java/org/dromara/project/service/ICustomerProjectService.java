package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.CustomerProjectBo;
import org.dromara.project.domain.vo.CustomerProjectVo;

import java.util.List;

public interface ICustomerProjectService {

    TableDataInfo<CustomerProjectVo> queryPageList(CustomerProjectBo bo, PageQuery pageQuery);

    List<CustomerProjectVo> queryList(CustomerProjectBo bo);

    CustomerProjectVo queryById(Long id);

    Long insertByBo(CustomerProjectBo bo);

    Boolean updateByBo(CustomerProjectBo bo);

    Boolean deleteById(Long id);
}
