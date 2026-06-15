SET NAMES utf8mb4;

-- --------------------------------------------------------
-- Part-time staff onboarding + WeChat miniapp client + roles + menus
-- Safe to run repeatedly. Existing staff profile data is preserved.
-- --------------------------------------------------------

-- ----------------------------
-- 1. Staff profile table
-- ----------------------------
create table if not exists pt_staff_profile
(
    profile_id              bigint(20)      not null                    comment 'profile primary id',
    tenant_id               varchar(20)     default '000000'            comment 'tenant id',
    user_id                 bigint(20)      not null                    comment 'sys_user id, unique per user',
    real_name               varchar(64)     default null                comment 'real name',
    phone                   varchar(32)     default null                comment 'mobile phone number',
    phone_verified          tinyint(1)      default 0                   comment '1=verified, 0=unverified',
    wechat_id               varchar(64)     default null                comment 'personal wechat id',
    region                  varchar(128)    default null                comment 'region/province+city',
    douyin_id               varchar(64)     default null                comment 'douyin display id, optional',
    inviter_user_id         bigint(20)      default null                comment 'inviter user id, separate from superior',
    onboarding_status       varchar(32)     default 'incomplete'        comment 'incomplete,pending,approved,rejected,disabled',
    audit_by                bigint(20)      default null                comment 'who approved or rejected',
    audit_at                datetime        default null                comment 'when approved or rejected',
    reject_reason           varchar(500)    default null                comment 'reason for rejection',
    remark                  varchar(500)    default null                comment 'general remark',
    create_dept             bigint(20)      default null                comment 'create department',
    create_by               bigint(20)      default null                comment 'create user',
    create_time             datetime        default null                comment 'create time',
    update_by               bigint(20)      default null                comment 'update user',
    update_time             datetime        default null                comment 'update time',
    del_flag                char(1)         default '0'                 comment 'delete flag: 0 active, 1 deleted',
    primary key (profile_id),
    unique key uk_pt_staff_user (tenant_id, user_id),
    key idx_pt_staff_status (tenant_id, onboarding_status),
    key idx_pt_staff_inviter (tenant_id, inviter_user_id)
) engine=innodb comment='part-time staff onboarding profile';

-- ----------------------------
-- 2. Miniapp client (sys_client)
-- ----------------------------
-- client_id is MD5 of 'miniapp123': ba9e8a5f68fd1436043780186727e92f
-- grant_type includes 'xcx' for WeChat mini program auth
-- token timeout: active 30min, max 7 days (suitable for mobile users)
insert into sys_client
    (id, client_id, client_key, client_secret, grant_type, device_type, active_timeout, timeout, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
values
    (3, 'ba9e8a5f68fd1436043780186727e92f', 'miniapp', 'miniapp123', 'xcx', 'xcx', 1800, 604800, '0', '0', 103, 1, sysdate(), 1, sysdate())
on duplicate key update
    client_id = values(client_id),
    client_key = values(client_key),
    client_secret = values(client_secret),
    grant_type = values(grant_type),
    device_type = values(device_type),
    active_timeout = values(active_timeout),
    timeout = values(timeout),
    status = values(status),
    del_flag = values(del_flag),
    update_by = values(update_by),
    update_time = values(update_time);

-- ----------------------------
-- 3. Roles
-- ----------------------------
-- pt_staff: part-time staff, data_scope=5 (self only), no admin menus
-- pt_auditor: part-time auditor, can review staff onboarding
insert into sys_role
    (role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (5, '000000', '兼职人员', 'pt_staff', 5, '5', 1, 1, '0', '0', 103, 1, sysdate(), 1, sysdate(), '兼职任务领取、作品提交和佣金查看')
on duplicate key update
    role_name = values(role_name),
    role_key = values(role_key),
    role_sort = values(role_sort),
    data_scope = values(data_scope),
    status = values(status),
    del_flag = values(del_flag),
    update_by = values(update_by),
    update_time = values(update_time),
    remark = values(remark);

insert into sys_role
    (role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (6, '000000', '兼职审核员', 'pt_auditor', 6, '5', 1, 1, '0', '0', 103, 1, sysdate(), 1, sysdate(), '审核兼职入驻和任务提交')
on duplicate key update
    role_name = values(role_name),
    role_key = values(role_key),
    role_sort = values(role_sort),
    data_scope = values(data_scope),
    status = values(status),
    del_flag = values(del_flag),
    update_by = values(update_by),
    update_time = values(update_time),
    remark = values(remark);

-- ----------------------------
-- 4. Admin menus (2200-2249)
-- ----------------------------
DELETE FROM sys_role_menu WHERE menu_id BETWEEN 2200 AND 2249;
DELETE FROM sys_menu WHERE menu_id BETWEEN 2200 AND 2249;

-- Directory
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2200, '兼职任务管理', 0, 5, 'parttime', NULL, '', 1, 0, 'M', '0', '0', '', 'peoples', 103, 1, SYSDATE(), NULL, NULL, '兼职人员、入驻审核和任务管理')
ON DUPLICATE KEY UPDATE
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_time = values(update_time),
    remark = values(remark);

-- Pages
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2201, '兼职人员', 2200, 1, 'staff', 'parttime/staff/index', '', 1, 0, 'C', '0', '0', 'parttime:staff:list', 'user', 103, 1, SYSDATE(), NULL, NULL, '兼职人员列表和入驻管理')
ON DUPLICATE KEY UPDATE
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_time = values(update_time),
    remark = values(remark);

-- Buttons under 兼职人员
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2210, '人员查询', 2201, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:staff:query', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2211, '审核通过', 2201, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:staff:approve', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2212, '审核驳回', 2201, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:staff:reject', '#', 103, 1, SYSDATE(), NULL, NULL, '')
ON DUPLICATE KEY UPDATE
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_time = values(update_time),
    remark = values(remark);

-- Assign to superadmin (role_id=1) and pt_auditor (role_id=6)
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2200 AND 2249;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 6, menu_id FROM sys_menu WHERE menu_id BETWEEN 2200 AND 2249;
