package org.dromara.creator.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.creator.domain.CmCreatorAccount;

/**
 * Creator account mapper.
 */
public interface CmCreatorAccountMapper extends BaseMapperPlus<CmCreatorAccount, CmCreatorAccount> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "mt.owner_dept_id"),
        @DataColumn(key = "userName", value = "mt.owner_user_id"),
        @DataColumn(key = "subordinateName", value = "mt.owner_user_id")
    })
    Page<CmCreatorAccount> selectScopedPage(@Param("page") Page<CmCreatorAccount> page,
                                            @Param("query") CmCreatorAccount query);

    @DataPermission({
        @DataColumn(key = "deptName", value = "mt.owner_dept_id"),
        @DataColumn(key = "userName", value = "mt.owner_user_id"),
        @DataColumn(key = "subordinateName", value = "mt.owner_user_id")
    })
    CmCreatorAccount selectScopedById(@Param("creatorId") Long creatorId);
}
