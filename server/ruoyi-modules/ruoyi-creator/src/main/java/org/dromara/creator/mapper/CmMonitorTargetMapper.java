package org.dromara.creator.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.creator.domain.CmMonitorTarget;

import java.util.List;

/**
 * Monitor target mapper.
 */
public interface CmMonitorTargetMapper extends BaseMapperPlus<CmMonitorTarget, CmMonitorTarget> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "owner_dept_id"),
        @DataColumn(key = "userName", value = "owner_user_id"),
        @DataColumn(key = "subordinateName", value = "owner_user_id")
    })
    default Page<CmMonitorTarget> selectScopedPage(Page<CmMonitorTarget> page, Wrapper<CmMonitorTarget> wrapper) {
        return selectPage(page, wrapper);
    }

    @DataPermission({
        @DataColumn(key = "deptName", value = "owner_dept_id"),
        @DataColumn(key = "userName", value = "owner_user_id"),
        @DataColumn(key = "subordinateName", value = "owner_user_id")
    })
    default List<CmMonitorTarget> selectScopedList(Wrapper<CmMonitorTarget> wrapper) {
        return selectList(wrapper);
    }

    @DataPermission({
        @DataColumn(key = "deptName", value = "owner_dept_id"),
        @DataColumn(key = "userName", value = "owner_user_id"),
        @DataColumn(key = "subordinateName", value = "owner_user_id")
    })
    default CmMonitorTarget selectScopedOne(Wrapper<CmMonitorTarget> wrapper) {
        return selectOne(wrapper);
    }
}
