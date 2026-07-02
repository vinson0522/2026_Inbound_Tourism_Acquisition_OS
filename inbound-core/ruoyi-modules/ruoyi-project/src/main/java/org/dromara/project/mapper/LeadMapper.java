package org.dromara.project.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.project.domain.Lead;
import org.dromara.project.domain.vo.LeadVo;

public interface LeadMapper extends BaseMapperPlus<Lead, LeadVo> {

    @Select("""
        SELECT COALESCE(nick_name, user_name)
        FROM sys_user
        WHERE user_id = #{userId}
          AND del_flag = '0'
        LIMIT 1
        """)
    String selectUserDisplayName(@Param("userId") Long userId);
}
