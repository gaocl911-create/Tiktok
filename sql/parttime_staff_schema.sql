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
-- 1.1 Promotion task table
-- ----------------------------
create table if not exists pt_promotion_task
(
    task_id                 bigint(20)      not null                    comment 'task primary id',
    tenant_id               varchar(20)     default '000000'            comment 'tenant id',
    task_title              varchar(128)    not null                    comment 'task title',
    platform                varchar(32)     default 'douyin'            comment 'platform: douyin/xhs/etc.',
    task_desc               varchar(1000)   default null                comment 'task description',
    task_requirement        varchar(2000)   default null                comment 'publish/submission requirements',
    unit_price              decimal(10,2)   not null default 0.00       comment 'commission per approved submission',
    total_quota             int             not null default 0          comment 'total claim quota',
    claimed_count           int             not null default 0          comment 'claimed count',
    submitted_count         int             not null default 0          comment 'submitted count',
    approved_count          int             not null default 0          comment 'approved count',
    start_time              datetime        default null                comment 'task start time',
    end_time                datetime        default null                comment 'task end time',
    task_status             varchar(32)     default 'draft'             comment 'draft,published,paused,finished',
    publish_time            datetime        default null                comment 'publish time',
    pause_time              datetime        default null                comment 'pause time',
    finish_time             datetime        default null                comment 'finish time',
    remark                  varchar(500)    default null                comment 'remark',
    create_dept             bigint(20)      default null                comment 'create department',
    create_by               bigint(20)      default null                comment 'create user',
    create_time             datetime        default null                comment 'create time',
    update_by               bigint(20)      default null                comment 'update user',
    update_time             datetime        default null                comment 'update time',
    del_flag                char(1)         default '0'                 comment 'delete flag: 0 active, 1 deleted',
    primary key (task_id),
    key idx_pt_task_status (tenant_id, task_status),
    key idx_pt_task_time (tenant_id, start_time, end_time),
    key idx_pt_task_platform (tenant_id, platform)
) engine=innodb comment='part-time promotion task';

-- ----------------------------
-- 1.2 Task claim table
-- ----------------------------
create table if not exists pt_task_claim
(
    claim_id                bigint(20)      not null                    comment 'claim primary id',
    tenant_id               varchar(20)     default '000000'            comment 'tenant id',
    task_id                 bigint(20)      not null                    comment 'promotion task id',
    profile_id              bigint(20)      not null                    comment 'staff profile id',
    user_id                 bigint(20)      not null                    comment 'staff sys_user id',
    claim_status            varchar(32)     default 'claimed'           comment 'claimed,submitted,approved,rejected,cancelled',
    claim_time              datetime        default null                comment 'claim time',
    submit_time             datetime        default null                comment 'latest submit time',
    finish_time             datetime        default null                comment 'approved finish time',
    remark                  varchar(500)    default null                comment 'remark',
    create_dept             bigint(20)      default null                comment 'create department',
    create_by               bigint(20)      default null                comment 'create user',
    create_time             datetime        default null                comment 'create time',
    update_by               bigint(20)      default null                comment 'update user',
    update_time             datetime        default null                comment 'update time',
    del_flag                char(1)         default '0'                 comment 'delete flag: 0 active, 1 deleted',
    primary key (claim_id),
    unique key uk_pt_claim_task_user (tenant_id, task_id, user_id),
    key idx_pt_claim_profile (tenant_id, profile_id),
    key idx_pt_claim_status (tenant_id, claim_status)
) engine=innodb comment='part-time task claim record';

-- ----------------------------
-- 1.3 Task submission table
-- ----------------------------
create table if not exists pt_task_submission
(
    submission_id           bigint(20)      not null                    comment 'submission primary id',
    tenant_id               varchar(20)     default '000000'            comment 'tenant id',
    claim_id                bigint(20)      not null                    comment 'task claim id',
    task_id                 bigint(20)      not null                    comment 'promotion task id',
    profile_id              bigint(20)      not null                    comment 'staff profile id',
    user_id                 bigint(20)      not null                    comment 'staff sys_user id',
    platform                varchar(32)     default 'douyin'            comment 'platform: douyin/xhs/etc.',
    content_url             varchar(1000)   not null                    comment 'submitted content share url',
    content_desc            varchar(1000)   default null                comment 'staff submission description',
    screenshot_url          varchar(1000)   default null                comment 'optional screenshot url',
    submission_status       varchar(32)     default 'pending'           comment 'pending,approved,rejected',
    submit_time             datetime        default null                comment 'submit time',
    audit_by                bigint(20)      default null                comment 'audit user id',
    audit_time              datetime        default null                comment 'audit time',
    reject_reason           varchar(500)    default null                comment 'reject reason',
    monitor_content_id      bigint(20)      default null                comment 'cm_content_post.content_id after approve',
    monitor_target_id       bigint(20)      default null                comment 'cm_monitor_target.target_id after approve',
    monitor_run_id          bigint(20)      default null                comment 'cm_collection_run.run_id after approve',
    remark                  varchar(500)    default null                comment 'remark',
    create_dept             bigint(20)      default null                comment 'create department',
    create_by               bigint(20)      default null                comment 'create user',
    create_time             datetime        default null                comment 'create time',
    update_by               bigint(20)      default null                comment 'update user',
    update_time             datetime        default null                comment 'update time',
    del_flag                char(1)         default '0'                 comment 'delete flag: 0 active, 1 deleted',
    primary key (submission_id),
    key idx_pt_submission_task (tenant_id, task_id),
    key idx_pt_submission_claim (tenant_id, claim_id),
    key idx_pt_submission_user (tenant_id, user_id),
    key idx_pt_submission_status (tenant_id, submission_status),
    key idx_pt_submission_monitor (tenant_id, monitor_content_id, monitor_target_id)
) engine=innodb comment='part-time task content submission';

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

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2202, '兼职任务', 2200, 2, 'task', 'parttime/task/index', '', 1, 0, 'C', '0', '0', 'parttime:task:list', 'my-task', 103, 1, SYSDATE(), NULL, NULL, '兼职推广任务创建、发布和管理')
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

INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2203, '作品审核', 2200, 3, 'submission', 'parttime/submission/index', '', 1, 0, 'C', '0', '0', 'parttime:submission:list', 'form', 103, 1, SYSDATE(), NULL, NULL, '兼职人员提交作品审核和监测关联')
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

-- Buttons under 兼职任务
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2220, '任务查询', 2202, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:task:query', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2221, '任务新增', 2202, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:task:add', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2222, '任务编辑', 2202, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:task:edit', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2223, '任务发布', 2202, 4, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:task:publish', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2224, '任务暂停', 2202, 5, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:task:pause', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2225, '任务结束', 2202, 6, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:task:finish', '#', 103, 1, SYSDATE(), NULL, NULL, '')
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

-- Buttons under 作品审核
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES
    (2230, '提交查询', 2203, 1, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:submission:query', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2231, '审核通过', 2203, 2, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:submission:approve', '#', 103, 1, SYSDATE(), NULL, NULL, ''),
    (2232, '审核驳回', 2203, 3, '', NULL, '', 1, 0, 'F', '0', '0', 'parttime:submission:reject', '#', 103, 1, SYSDATE(), NULL, NULL, '')
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
