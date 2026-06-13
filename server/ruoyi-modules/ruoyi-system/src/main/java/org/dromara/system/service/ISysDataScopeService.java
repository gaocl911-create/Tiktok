package org.dromara.system.service;

/**
 * 通用 数据权限 服务
 *
 * @author Lion Li
 */
public interface ISysDataScopeService {

    /**
     * 获取角色自定义权限
     *
     * @param roleId 角色id
     * @return 部门id组
     */
    String getRoleCustom(Long roleId);

    /**
     * 获取部门及以下权限
     *
     * @param deptId 部门id
     * @return 部门id组
     */
    String getDeptAndChild(Long deptId);

    /**
     * Get the current user and all recursively subordinate user IDs.
     *
     * @param userId current user ID
     * @param tenantId tenant ID
     * @return comma-separated user IDs
     */
    String getSelfAndSubordinateUsers(Long userId, String tenantId);

}
