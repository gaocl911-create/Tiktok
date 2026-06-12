package org.dromara.creator.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.creator.domain.CmAlertEvent;

public interface CmAlertEventMapper extends BaseMapperPlus<CmAlertEvent, CmAlertEvent> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "mt.owner_dept_id"),
        @DataColumn(key = "userName", value = "mt.owner_user_id"),
        @DataColumn(key = "superiorName", value = "COALESCE(mt.direct_superior_user_id,"
            + " (SELECT leader FROM sys_dept WHERE dept_id = mt.owner_dept_id))")
    })
    Page<CmAlertEvent> selectScopedPage(@Param("page") Page<CmAlertEvent> page,
                                        @Param("query") CmAlertEvent query);

    @DataPermission({
        @DataColumn(key = "deptName", value = "mt.owner_dept_id"),
        @DataColumn(key = "userName", value = "mt.owner_user_id"),
        @DataColumn(key = "superiorName", value = "COALESCE(mt.direct_superior_user_id,"
            + " (SELECT leader FROM sys_dept WHERE dept_id = mt.owner_dept_id))")
    })
    CmAlertEvent selectScopedById(@Param("eventId") Long eventId);
}
