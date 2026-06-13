package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.domain.SysRoleDept;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleDeptMapper;
import org.dromara.system.service.ISysDataScopeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 数据权限 实现
 * <p>
 * 注意: 此Service内不允许调用标注`数据权限`注解的方法
 * 例如: deptMapper.selectList 此 selectList 方法标注了`数据权限`注解 会出现循环解析的问题
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service("sdss")
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取角色自定义权限
     *
     * @param roleId 角色Id
     * @return 部门Id组
     */
    @Cacheable(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#roleId", condition = "#roleId != null")
    @Override
    public String getRoleCustom(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return "-1";
        }
        List<SysRoleDept> list = roleDeptMapper.selectList(
            new LambdaQueryWrapper<SysRoleDept>()
                .select(SysRoleDept::getDeptId)
                .eq(SysRoleDept::getRoleId, roleId));
        if (CollUtil.isNotEmpty(list)) {
            return StreamUtils.join(list, rd -> Convert.toStr(rd.getDeptId()));
        }
        return "-1";
    }

    /**
     * 获取部门及以下权限
     *
     * @param deptId 部门Id
     * @return 部门Id组
     */
    @Cacheable(cacheNames = CacheNames.SYS_DEPT_AND_CHILD, key = "#deptId", condition = "#deptId != null")
    @Override
    public String getDeptAndChild(Long deptId) {
        if (ObjectUtil.isNull(deptId)) {
            return "-1";
        }
        List<Long> deptIds = deptMapper.selectDeptAndChildById(deptId);
        return CollUtil.isNotEmpty(deptIds) ? StreamUtils.join(deptIds, Convert::toStr) : "-1";
    }

    @Override
    public String getSelfAndSubordinateUsers(Long userId, String tenantId) {
        if (ObjectUtil.isNull(userId) || StringUtils.isBlank(tenantId)) {
            return "-1";
        }

        Set<Long> result = new LinkedHashSet<>();
        Set<Long> frontier = new LinkedHashSet<>();
        result.add(userId);
        frontier.add(userId);

        for (int depth = 0; depth < 100 && CollUtil.isNotEmpty(frontier); depth++) {
            String placeholders = String.join(",", Collections.nCopies(frontier.size(), "?"));
            String sql = """
                SELECT DISTINCT owner_user_id
                FROM cm_monitor_target
                WHERE tenant_id = ?
                  AND owner_user_id IS NOT NULL
                  AND direct_superior_user_id IN (%s)
                """.formatted(placeholders);

            List<Object> args = new ArrayList<>(frontier.size() + 1);
            args.add(tenantId);
            args.addAll(frontier);
            List<Long> children = jdbcTemplate.queryForList(sql, Long.class, args.toArray());

            Set<Long> next = new LinkedHashSet<>();
            for (Long child : children) {
                if (ObjectUtil.isNotNull(child) && result.add(child)) {
                    next.add(child);
                }
            }
            frontier = next;
        }

        StringJoiner joiner = new StringJoiner(",");
        result.forEach(id -> joiner.add(id.toString()));
        return joiner.toString();
    }

}
