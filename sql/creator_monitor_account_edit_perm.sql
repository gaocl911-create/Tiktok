insert into sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_dept, create_by, create_time, update_by, update_time, remark
)
select 2233, '账号编辑', 2101, 4, '#', null, null,
       1, 0, 'F', '0', '0', 'creator:account:edit', '#',
       null, 1, now(), null, null, ''
where not exists (
    select 1 from sys_menu where perms = 'creator:account:edit'
);

insert ignore into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, m.menu_id
from sys_role_menu rm
join sys_menu add_menu on add_menu.menu_id = rm.menu_id
join sys_menu m on m.perms = 'creator:account:edit'
where add_menu.perms = 'creator:account:add';
