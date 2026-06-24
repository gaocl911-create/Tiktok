SET NAMES utf8mb4;

-- --------------------------------------------------------
-- Part-time task material library
-- Safe to run repeatedly. Existing part-time task data is preserved.
-- --------------------------------------------------------

create table if not exists pt_material_category
(
    category_id     bigint(20)   not null                 comment 'category primary id',
    tenant_id       varchar(20)  default '000000'         comment 'tenant id',
    category_type   varchar(16)  not null                 comment 'text,image',
    category_name   varchar(128) not null                 comment 'category name',
    sort            int          default 0                comment 'sort order',
    status          char(1)      default '0'              comment '0 enabled, 1 disabled',
    remark          varchar(500) default null             comment 'remark',
    create_dept     bigint(20)   default null             comment 'create department',
    create_by       bigint(20)   default null             comment 'create user',
    create_time     datetime     default null             comment 'create time',
    update_by       bigint(20)   default null             comment 'update user',
    update_time     datetime     default null             comment 'update time',
    del_flag        char(1)      default '0'              comment 'delete flag: 0 active, 1 deleted',
    primary key (category_id),
    key idx_pt_material_category_type (tenant_id, category_type, status, sort)
) engine=innodb comment='part-time material category';

create table if not exists pt_material_text
(
    text_id       bigint(20)    not null                  comment 'text material primary id',
    tenant_id     varchar(20)   default '000000'          comment 'tenant id',
    category_id   bigint(20)    not null                  comment 'pt_material_category.category_id',
    content       varchar(3000) not null                  comment 'copy text content',
    sort          int           default 0                 comment 'sort order',
    status        char(1)       default '0'               comment '0 enabled, 1 disabled',
    remark        varchar(500)  default null              comment 'remark',
    create_dept   bigint(20)    default null              comment 'create department',
    create_by     bigint(20)    default null              comment 'create user',
    create_time   datetime      default null              comment 'create time',
    update_by     bigint(20)    default null              comment 'update user',
    update_time   datetime      default null              comment 'update time',
    del_flag      char(1)       default '0'               comment 'delete flag: 0 active, 1 deleted',
    primary key (text_id),
    key idx_pt_material_text_category (tenant_id, category_id, status, sort, text_id)
) engine=innodb comment='part-time text material';

create table if not exists pt_material_image
(
    image_id      bigint(20)    not null                  comment 'image material primary id',
    tenant_id     varchar(20)   default '000000'          comment 'tenant id',
    category_id   bigint(20)    not null                  comment 'pt_material_category.category_id',
    image_url     varchar(1000) not null                  comment 'image url',
    image_name    varchar(255)  default null              comment 'image display name',
    image_size    bigint(20)    default null              comment 'image size in bytes',
    sort          int           default 0                 comment 'sort order',
    status        char(1)       default '0'               comment '0 enabled, 1 disabled',
    remark        varchar(500)  default null              comment 'remark',
    create_dept   bigint(20)    default null              comment 'create department',
    create_by     bigint(20)    default null              comment 'create user',
    create_time   datetime      default null              comment 'create time',
    update_by     bigint(20)    default null              comment 'update user',
    update_time   datetime      default null              comment 'update time',
    del_flag      char(1)       default '0'               comment 'delete flag: 0 active, 1 deleted',
    primary key (image_id),
    key idx_pt_material_image_category (tenant_id, category_id, status, sort, image_id)
) engine=innodb comment='part-time image material';

create table if not exists pt_task_material_config
(
    config_id        bigint(20)  not null                 comment 'config primary id',
    tenant_id        varchar(20) default '000000'         comment 'tenant id',
    task_id          bigint(20)  not null                 comment 'pt_promotion_task.task_id',
    text_category_id bigint(20)  default null             comment 'text category id',
    image_category_id bigint(20) default null             comment 'image category id',
    assign_mode      varchar(32) default 'sequence_loop'  comment 'assignment mode',
    create_dept      bigint(20)  default null             comment 'create department',
    create_by        bigint(20)  default null             comment 'create user',
    create_time      datetime    default null             comment 'create time',
    update_by        bigint(20)  default null             comment 'update user',
    update_time      datetime    default null             comment 'update time',
    del_flag         char(1)     default '0'              comment 'delete flag: 0 active, 1 deleted',
    primary key (config_id),
    unique key uk_pt_task_material_config_task (tenant_id, task_id)
) engine=innodb comment='part-time task material config';

create table if not exists pt_task_material_assignment
(
    assignment_id       bigint(20)    not null             comment 'assignment primary id',
    tenant_id           varchar(20)   default '000000'     comment 'tenant id',
    task_id             bigint(20)    not null             comment 'pt_promotion_task.task_id',
    claim_id            bigint(20)    not null             comment 'pt_task_claim.claim_id',
    user_id             bigint(20)    not null             comment 'staff sys_user id',
    assign_index        int           not null             comment 'claim sequence number',
    text_id             bigint(20)    default null         comment 'pt_material_text.text_id',
    text_snapshot       varchar(3000) default null         comment 'assigned text snapshot',
    image_id            bigint(20)    default null         comment 'pt_material_image.image_id',
    image_url_snapshot  varchar(1000) default null         comment 'assigned image url snapshot',
    image_name_snapshot varchar(255)  default null         comment 'assigned image name snapshot',
    create_dept         bigint(20)    default null         comment 'create department',
    create_by           bigint(20)    default null         comment 'create user',
    create_time         datetime      default null         comment 'create time',
    update_by           bigint(20)    default null         comment 'update user',
    update_time         datetime      default null         comment 'update time',
    del_flag            char(1)       default '0'          comment 'delete flag: 0 active, 1 deleted',
    primary key (assignment_id),
    unique key uk_pt_task_assignment_claim (tenant_id, claim_id),
    key idx_pt_task_assignment_task (tenant_id, task_id, assign_index),
    key idx_pt_task_assignment_user (tenant_id, user_id)
) engine=innodb comment='part-time task material assignment';

-- --------------------------------------------------------
-- Admin menus. Uses 2250-2279 to avoid the existing 2200-2249 bootstrap range.
-- --------------------------------------------------------

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (2250, '素材分类', 2200, 4, 'material/category', 'parttime/material/category/index', '', 1, 0, 'C', '0', '0', 'parttime:material:category:list', 'tree-table', 103, 1, sysdate(), null, null, '兼职任务素材分类管理'),
    (2260, '文案库', 2200, 5, 'material/text', 'parttime/material/text/index', '', 1, 0, 'C', '0', '0', 'parttime:material:text:list', 'documentation', 103, 1, sysdate(), null, null, '兼职任务文案素材管理'),
    (2270, '图片库', 2200, 6, 'material/image', 'parttime/material/image/index', '', 1, 0, 'C', '0', '0', 'parttime:material:image:list', 'image', 103, 1, sysdate(), null, null, '兼职任务图片素材管理')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_time = values(update_time),
    remark = values(remark);

insert into sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
    (2251, '分类查询', 2250, 1, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:category:query', '#', 103, 1, sysdate(), null, null, ''),
    (2252, '分类新增', 2250, 2, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:category:add', '#', 103, 1, sysdate(), null, null, ''),
    (2253, '分类编辑', 2250, 3, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:category:edit', '#', 103, 1, sysdate(), null, null, ''),
    (2254, '分类删除', 2250, 4, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:category:remove', '#', 103, 1, sysdate(), null, null, ''),
    (2261, '文案查询', 2260, 1, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:text:query', '#', 103, 1, sysdate(), null, null, ''),
    (2262, '文案新增', 2260, 2, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:text:add', '#', 103, 1, sysdate(), null, null, ''),
    (2263, '文案编辑', 2260, 3, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:text:edit', '#', 103, 1, sysdate(), null, null, ''),
    (2264, '文案删除', 2260, 4, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:text:remove', '#', 103, 1, sysdate(), null, null, ''),
    (2271, '图片查询', 2270, 1, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:image:query', '#', 103, 1, sysdate(), null, null, ''),
    (2272, '图片新增', 2270, 2, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:image:add', '#', 103, 1, sysdate(), null, null, ''),
    (2273, '图片编辑', 2270, 3, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:image:edit', '#', 103, 1, sysdate(), null, null, ''),
    (2274, '图片删除', 2270, 4, '', null, '', 1, 0, 'F', '0', '0', 'parttime:material:image:remove', '#', 103, 1, sysdate(), null, null, '')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    update_time = values(update_time),
    remark = values(remark);

insert ignore into sys_role_menu (role_id, menu_id) select 1, menu_id from sys_menu where menu_id between 2250 and 2279;
insert ignore into sys_role_menu (role_id, menu_id) select 6, menu_id from sys_menu where menu_id between 2250 and 2279;

-- Docker 部署时，API 容器内的 127.0.0.1 指向 API 自身，不是 MinIO。
-- 后端用 Docker 服务名访问 MinIO；前端预览图片用宿主机映射端口。
update sys_oss_config
set endpoint = 'minio:9000',
    domain = 'localhost:19000',
    is_https = 'N',
    update_time = sysdate()
where config_key = 'minio'
  and endpoint in ('127.0.0.1:9000', 'localhost:9000');
