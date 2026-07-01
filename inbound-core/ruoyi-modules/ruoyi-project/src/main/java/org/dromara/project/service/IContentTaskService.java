package org.dromara.project.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.project.domain.bo.ContentGenerateBo;
import org.dromara.project.domain.bo.ContentTaskBo;
import org.dromara.project.domain.vo.ContentGenerateVo;
import org.dromara.project.domain.vo.ContentTaskDetailVo;
import org.dromara.project.domain.vo.ContentTaskVo;

public interface IContentTaskService {

    TableDataInfo<ContentTaskVo> queryPageList(Long projectId, ContentTaskBo bo, PageQuery pageQuery);

    ContentTaskDetailVo queryById(Long projectId, Long taskId);

    Long insertByBo(Long projectId, ContentTaskBo bo);

    Boolean deleteById(Long projectId, Long taskId);

    ContentGenerateVo generate(Long projectId, Long taskId, ContentGenerateBo bo);
}
